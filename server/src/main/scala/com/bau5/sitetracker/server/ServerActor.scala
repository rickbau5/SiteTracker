package com.bau5.sitetracker.server

import java.io.{File, PrintWriter}

import akka.actor.{ActorPath, ActorIdentity, Identify, Actor}
import com.bau5.sitetracker.common.AnomalyDetails
import com.bau5.sitetracker.common.AnomalyDetails._
import com.bau5.sitetracker.common.Events._
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source
import scala.util.{Try, Success}

/**
  * Created by Rick on 1/15/16.
  */
class ServerActor extends Actor {
  var entries = mutable.Map.empty[SSystem, List[AnomalyEntry]]
  var actors = mutable.ListBuffer.empty[ActorPath]

  var saveLocation: String = context.system.settings.config.getString("server.save-location")

  override def receive: Receive = {
    case Login(user) =>
      println(s"User [$user] logged in.")
      sender ! Message(s"Welcome [${user.value}].")

    case Logout(user) =>
      println(s"User [$user] logged out.")
      sender ! Message(s"Logged [${user.value}] out.")

    case Connect(ref) =>
      println(s"Got a new connection [$ref]")
      actors += ref.path
      println(ref.path)
      sender ! ConnectionSuccessful()

    case Disconnect(ref) =>
      println(s"Client disconnect [$ref]")
      actors -= ref.path
      println("Active actors is now" + actors)

    case Quit(ref) =>
      println(s"[$ref] is quitting.")
      saveMap(entries, saveLocation)
      actors -= ref.path
      sender ! Message(s"Successfully disassociated.")

    case MessageAll(msg) =>
      println(s"Sending [$msg] to all clients.")
      actors foreach (e => context.actorSelection(e) ! msg)

    case Message(msg) =>
      println(s"Got message [$msg] from [$sender]")

    case ServerMessage(msg) =>
      println(s"Got [$msg] from [$sender]")
      if (msg == "Ping") sender ! Message("Pong")

    case DownloadEntriesRequest =>
      println(s"Got download request from [$sender]")
      sender ! DownloadEntriesResponse(
        entries.flatMap { group =>
          group._2.map(entry => s"${group._1.value},${entry.stringify}\n")
        }.toList
      )

    case ListSystemsRequest =>
      println(s"Got list request from [$sender]")
      sender ! ListSystemsResponse(entries.map(e => (e._1, e._2.size)).toList)

    case SeeSystemRequest(sys) =>
      val ret = entries.get(SSystem(sys))
      println(s"Got system request for [$sys] from [$sender]. Sending response: " + ret)
      sender ! SeeSystemResponse(ret)

    case SeeAllSystemsRequest =>
      val ret = if (entries.nonEmpty) Option(entries.toList) else None
      println(s"Got see all request from [$sender].")
      sender ! SeeAllSystemsResponse(ret)

    case FindEntriesRequest(attributes) =>
      // Filters out all anomalies not containing the anomaly detail provided
      def filterEntries(detail: AnomalyDetail, list: List[AnomalyEntry]): List[AnomalyEntry] = detail match {
        case name: Name => list.filter(_.anomaly.name == name)
        case id: Identifier => list.filter(_.anomaly.ident == id)
        case typ: Type => list.filter(_.anomaly.typ == typ)
        case user: User => list.filter(_.user == user)
        case _ => List.empty[AnomalyEntry]    // Unsupported (Time, SSystem)
      }

      val matched = attributes.tail
        .foldLeft(entries.map { entry =>                          // Initial filter
            entry._1 -> filterEntries(attributes.head, entry._2)
          }.toList
        ) { case (matches, attribute) =>                          // Fold, filtering the list for each attribute
          matches.map(entry => entry._1 -> filterEntries(attribute, entry._2))
        }.filter(_._2.nonEmpty)

      // If no entry matched all requirements, return None else return matches
      val returned = if (matched.nonEmpty) Option(matched) else None

      sender ! FindEntriesResponse(returned)

    case SaveRequest =>
      println(s"Got save request from [$sender]")
      saveMap(entries, saveLocation)
      sender ! Message("Saved successfully.")

    case mut: Request if mut.isInstanceOf[Mutator] =>
      receiveMutator(mut)

    case LoadSavedData =>
      println(s"Loading data from [$saveLocation)]")
      loadMap(saveLocation)
      if (entries.nonEmpty) {
        println(s"Loaded ${entries.map(_._2.size).sum} entries.")
      }

    case _ =>
      println("Unhandled type.")
      sender ! Message("Couldn't complete the request.")
  }

  def receiveMutator: Receive = {
    val ret = handleMutator
    saveMap(entries, saveLocation)
    ret
  }

  def handleMutator: Receive = {
    case AddEntryRequest(entry) =>
      println(s"Got add entry request for [$entry] from [$sender]")
      val response = addEntry(entry, entries)
      if (response.isDefined) {
        println("Entry added.")
      } else {
        println("Entry was not added.")
      }
      sender ! AddEntryResponse(response.isDefined)


    case EditEntryRequest(entry: (SSystem, Identifier), attributes) =>
      println(s"Got edit entry request for [$entry] with attributes [$attributes]")
      val updated = entries.get(entry._1) match {   // Find entry matching the system
        case None =>
          Option.empty[AnomalyEntry]
        case Some(sys) =>
          sys.find(_.anomaly.ident == entry._2) match {   // Find entry with that ID
            case None =>
              Option.empty[AnomalyEntry]
            case Some(orig) =>
              val system = attributes.find(_.isInstanceOf[SSystem])   // Get new system attrib if present
                .map(_.asInstanceOf[SSystem])
                .getOrElse(entry._1)

              // Update the matched entry with new attributes
              val ret = attributes.filter(_ != system).foldLeft(orig) { case (ent, attribute) =>
                ent.update(attribute)
              }

              removeEntry(entry, entries)         // Remove the old entry
              addEntry(system -> ret, entries)    // Add the updated entry
              Option(ret)
          }
      }
      // Send response, None if nothing updated, Some(entry) of updated entry.
      sender ! EditEntryResponse(updated)

    case RemoveEntryRequest(entry) =>
      println(s"Got remove entry request for [$entry)] from [$sender]")
      val ret = removeEntry(entry, entries)
      sender ! RemoveEntryResponse(ret.isDefined)

    case RemoveAllEntriesRequest(system) =>
      println(s"Got add entry request for [$system] from [$sender]")
      val old = entries.remove(system)
      println(s"Removed these entries for [$system] $old")
      sender ! RemoveAllEntriesResponse(old.isDefined)
  }

  def addEntry(newEntry: (SSystem, AnomalyEntry), map: mutable.Map[SSystem, List[AnomalyEntry]]): Option[List[AnomalyEntry]] = {
    val newValue = map.getOrElse(newEntry._1, List.empty[AnomalyEntry]) match {   // Find the system, or prepare empty list
      case Nil =>
        println("Adding " + newEntry._2)
        List(newEntry._2)
      case list if !list.map(e => e.anomaly.ident).contains(newEntry._2.anomaly.ident) => // Check if system has an entry by that id
        println("Adding " + newEntry._2)
        (list ++ List(newEntry._2)).sortBy(_.anomaly.ident.value)    // Sort list and return
      case list =>
        // Entry is already present, do nothing.
        println(s"Anomaly already logged.")
        List.empty
    }

    // If an entry was added, update map and return new list, else return none indicating no change.
    if (newValue.nonEmpty) {
      map += (newEntry._1 -> newValue)
      Option(newValue)
    } else {
      None
    }
  }

  def removeEntry(entry: (SSystem, Identifier), map: mutable.Map[SSystem, List[AnomalyEntry]]): Option[List[AnomalyEntry]] = {
    entries.get(entry._1) match {
      case None =>
        None
      case Some(list) =>
        val newEntries = list.filter(_.anomaly.ident != entry._2)
        if (newEntries.size == list.size) {
          None
        } else {
          if (newEntries.nonEmpty) {
            entries += entry._1 -> newEntries
            Option(newEntries)
          } else {
            entries.remove(entry._1)
          }
        }
    }
  }

  def saveMap(map: mutable.Map[SSystem, List[AnomalyEntry]], file: String) = {
    val header = "system,id,name,type,user,time\n"
    val output = map.flatMap(group => group._2.map(b => s"${group._1.value},${b.stringify}\n"))

    val writer = new PrintWriter(new File(file))
    writer.write(header)
    output foreach (e => writer.write(e))
    writer.close()

    println("Saved map.")
  }

  def loadMap(source: String) = {
    val loaded = Source.fromFile(source)
      .getLines.toList.tail
      .map(_.split(","))
      .map { line =>
        val time = Try(AnomalyDetails.formatter.parseDateTime(line(5))).recover {
          case ex => new DateTime(line(5).toLong)
        }
        println(time.get)
        SSystem(line(0)) ->
          AnomalyEntry(
            User(line(4)),
            Anomaly(
              Identifier(line(1)),
              Name(line(2)),
              Type(line(3))
            ),
            Time(time.get)
          )
      }.groupBy(_._1)
      .map(kv => kv._1 -> kv._2.map(_._2))
    entries = mutable.Map(loaded.toSeq: _*)
  }
}

case object LoadSavedData