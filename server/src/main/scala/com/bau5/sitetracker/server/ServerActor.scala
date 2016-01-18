package com.bau5.sitetracker.server

import java.io.{File, PrintWriter}

import akka.actor.{ActorIdentity, Identify, Actor}
import com.bau5.sitetracker.common.EntryDetails._
import com.bau5.sitetracker.common.Events._
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source
import scala.util.Success

/**
  * Created by Rick on 1/15/16.
  */
class ServerActor extends Actor {
  var entries = mutable.Map.empty[SSystem, List[AnomalyEntry]]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    loadMap("/Users/Rick/testOutput.tsv")
  }

  override def receive: Receive = {
    case Login(user) =>
      println(s"User [$user] connected.")
      sender ! Message(s"Welcome [${user.username}].")

    case Logout(user) =>
      println(s"User [$user] disconnected.")
      sender ! Message(s"Logged [${user.username}] out.")

    case Connect() =>
      println(s"Got a new connection [$sender]")
      sender ! ConnectionSuccessful()

    case Message(msg) =>
      println(s"Got message [$msg] from [$sender]")

    case ServerMessage(msg) =>
      println(s"Got [$msg] from [$sender]")
      if (msg == "Ping") sender ! Message("Pong")

    case DownloadEntriesRequest() =>
      println(s"Got download request from [$sender]")
      sender ! DownloadEntriesResponse(
        entries.flatMap { group =>
          group._2.map(entry => s"${group._1.name},${entry.stringify}\n")
        }.toList
      )

    case ListSystemsRequest() =>
      println(s"Got list request from [$sender]")
      sender ! ListSystemsResponse(entries.map(e => (e._1, e._2.size)).toList)

    case SeeSystemRequest(sys) =>
      val ret = entries.get(SSystem(sys))
      println(s"Got system request for [$sys] from [$sender]. Sending response: " + ret)
      sender ! SeeSystemResponse(ret)

    case SaveRequest() =>
      println(s"Got save request from [$sender]")
      saveMap(entries)
      sender ! Message("Saved successfully.")

    case mut: Request if mut.isInstanceOf[Mutator] =>
      receiveMutator(mut)

    case _ =>
      println("Unhandled type.")
      sender ! Message("Couldn't complete the request.")
  }

  def receiveMutator: Receive = {
    val ret = handleMutator
    saveMap(entries)
    ret
  }

  def handleMutator: Receive = {
    case AddEntryRequest(entry) =>
      println(s"Got add entry request for [$entry] from [$sender]")
      addEntry(entry, entries)
      println("Successfully added entry.")
      sender ! AddEntryResponse(true)

    case RemoveEntryRequest((system, ident)) =>
      println(s"Got remove entry request for [$system-$ident)] from [$sender]")
      entries.get(system) match {
        case None =>
          sender ! RemoveEntryResponse(false)
        case Some(list) =>
          val newEntries = list.filter(_.anomaly.ident != ident)
          entries += system -> newEntries
          sender ! RemoveEntryResponse(newEntries.size != list.size)
      }

    case RemoveAllEntriesRequest(system) =>
      println(s"Got add entry request for [$system] from [$sender]")
      val old = entries.remove(system)
      println(s"Removed these entries for [$system] $old")
      sender ! RemoveAllEntriesResponse(old.isDefined)
  }

  def addEntry(newEntry: (SSystem, AnomalyEntry), map: mutable.Map[SSystem, List[AnomalyEntry]]) = {
    val newValue = map.getOrElse(newEntry._1, List.empty[AnomalyEntry]) match {
      case Nil =>
        println("Adding " + newEntry._2)
        List(newEntry._2)
      case list if !list.map(e => e.anomaly).contains(newEntry._2.anomaly) =>
        println("Adding " + newEntry._2)
        (list ++ List(newEntry._2)).sortBy(_.anomaly.ident.id)
      case list =>
        println(s"Anomaly already logged. New '${newEntry._2}', old '${list.map(e => e.anomaly).filter(_ == newEntry._2)}'")
        list
    }
    map += (newEntry._1 -> newValue)
  }

  def saveMap(map: mutable.Map[SSystem, List[AnomalyEntry]]) = {
    val header = "system,id,name,type,user,time\n"
    val output = map.flatMap(group => group._2.map(b => s"${group._1.name},${b.stringify}\n"))

    val writer = new PrintWriter(new File("/Users/Rick/testOutput.tsv"))
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
        SSystem(line(0)) ->
          AnomalyEntry(
            User(line(4)),
            Anomaly(
              Identifier(line(1)),
              Name(line(2)),
              Type(line(3))
            ),
            new DateTime(line(5).toLong)
          )
      }.groupBy(_._1)
      .map(kv => kv._1 -> kv._2.map(_._2))
    entries = mutable.Map(loaded.toSeq: _*)
  }
}
