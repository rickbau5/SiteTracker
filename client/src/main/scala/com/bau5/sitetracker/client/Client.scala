package com.bau5.sitetracker.client

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

import akka.actor.{Props, PoisonPill}
import akka.util.Timeout
import com.bau5.sitetracker.common.EntryDetails._
import com.bau5.sitetracker.common.Events._
import com.bau5.sitetracker.common.BaseProvider
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

/**
  * Created by Rick on 1/17/16.
  */
class Client(systemName: String = "ClientSystem",
             configName: String = "") extends BaseProvider(systemName, configName) {
  implicit val timeout = Timeout(5 seconds)

  val seeSystem = "see (.+)".r
  val addEntry = raw"add (.+) (\w{3}-\d{3}) ('.+') (.+)".r
  val removeOneEntry = raw"remove (.+) (\w{3}-\d{3})".r
  val removeAllEntries = "remove all (.+)".r
  val login = "login (.+)".r

  def inputLoop(): Unit = {
    var user = User("system")

    val driver = actorSystem.actorOf(Props[ClientActor], "clientDriver")
    Try(await[ConnectionSuccessful](Connect(driver), driver)) match {
      case Failure(ex) => println("Server not responding.")
      case Success(_) => println("Connected to server.");
    }

    while (true) Try(StdIn.readLine("> ") match {
      case login(username) =>
        user = User(username)
        val response = await[Message](Login(user), driver)
        println(response.contents)

      case _ if user.username == "system" =>
        println("Please login first.\nlogin [username]")

      case "logout" =>
        val response = await[Message](Logout(user), driver)
        println(response.contents)
        user = User("system")

      // Lookup the entries in a system.
      case seeSystem(sys) =>
        val response = await[SeeSystemResponse](SeeSystemRequest(sys), driver)
        response.entries match {
          case None => println("None")
          case Some(entries) =>
            println(sys)
            entries foreach (e => println(s" :$e"))
        }

      // List all systems
      case "list" =>
        val response = await[ListSystemsResponse](ListSystemsRequest, driver)
        println(response.systems.map(e => s" :${e._1.name} - ${e._2}").mkString("\n"))

      // Add a new entry
      case addEntry(system, id, name, typ) =>
        val newEntry = SSystem(system) -> AnomalyEntry(
          user,
          Anomaly(Identifier(id), Name(name.filterNot(_ == ''')), Type(typ)),
          DateTime.now()
        )
        await[AddEntryResponse](AddEntryRequest(newEntry), driver)
          .wasSuccessful match {
            case true => println(s"+${newEntry._1.name} ${newEntry._2}")
            case false => println("Entry was not added.")
          }

      // Remove Entry
      case removeOneEntry(system, id) =>
        await[RemoveEntryResponse](
            RemoveEntryRequest((SSystem(system), Identifier(id))),
            driver
          ).wasSuccessful match {
            case true => println("Removed entry.")
            case false => println("Entry was not removed. Not found?")
          }

      // Remove all entries for a system
      case removeAllEntries(system) =>
        await[RemoveAllEntriesResponse](
            RemoveAllEntriesRequest(SSystem(system)),
            driver
          ).wasSuccessful match {
            case true => println("Entries removed.")
            case false => println("Entries were not removed. System not found?")
          }

      // Download the current list of entries
      case "download" =>
        val ret = await[DownloadEntriesResponse](
            DownloadEntriesRequest,
            driver
          ).entries
        val selection = new StringSelection(ret.mkString(""))
        Toolkit.getDefaultToolkit.getSystemClipboard.setContents(selection, selection)
        println("All entries copied to clipboard.")

      // Quit
      case "quit" =>
        println(s"Bye ${user.username}")
        await[Message](Quit(driver), driver)
        driver ! PoisonPill
        sys.exit(0)

      case "save" =>
        val ret = await[Message](SaveRequest, driver)
        println(ret.contents)

      // Default and unknown case
      case _ => println("Unknown input, try again.")
    }) match {
      case Success(_) =>
      case Failure(_) => println("Failed parsing input.")
    }
  }
}
