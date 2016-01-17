package com.bau5.sitetracker.server

import java.io.{File, PrintWriter}

import akka.actor.Actor
import com.bau5.sitetracker.common.EntryDetails.{AnomalyEntry, SSystem, User}
import com.bau5.sitetracker.common.Events._

import scala.collection.mutable
import scala.util.Success

/**
  * Created by Rick on 1/15/16.
  */
class SiteTrackerServerActor extends Actor {
  var entries = mutable.Map.empty[SSystem, List[AnomalyEntry]]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
//    populateTestInput
  }

  override def receive: Receive = {
    case Connect(user) =>
      println(s"User [$user] connected.")
      sender ! Message(s"Welcome [${user.username}].")

    case Disconnect(user) =>
      println(s"User [$user] disconnected.")
      sender ! Message(s"Logged [${user.username}] out.")

    case Message(msg) =>
      println(s"Driver got $msg")

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

  def populateTestInput = {
    val testInput = List(
      "Tamo RMZ-630 'Provisional Gurista Outpost' Combat",
      "Tamo XKR-091 'Regional Guristas Data Processing Center' Data",
      "Tamo GGU-898 'Decayed Guristas Mass Grave' Relic",
      "Dabas XXA-444 'Guristas Port Authority' Combat",
      "Dabas WOR-123 'NA' Wormhole"
    )
//    val successful = testInput.map(input => com.bau5.sitetracker.Main.parseEntry(input, User("TestUser")))
//      .collect { case Success(v) => v }
//    successful foreach (e => addEntry(e, entries))
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
}
