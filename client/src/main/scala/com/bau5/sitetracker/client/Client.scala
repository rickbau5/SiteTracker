package com.bau5.sitetracker.client

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

import akka.actor.{Props, PoisonPill}
import akka.util.Timeout
import com.bau5.sitetracker.common.EntryDetails._
import com.bau5.sitetracker.common.Events._
import com.bau5.sitetracker.common.BaseProvider
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

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
      // Quit
      case "quit" =>
        if (user.username != "system") {
          println(s"Bye ${user.username}")
        }
        await[Message](Quit(driver), driver)
        driver ! PoisonPill
        sys.exit(0)

      // Login
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
            val list = entries map { entry =>
              val formatter = DateTimeFormat.forPattern("HH:mm:ss MM/dd/yyyy")
              List(entry.anomaly.ident.id, entry.anomaly.name.name, entry.anomaly.typ.str,
                    entry.timeRecorded.toString(formatter), entry.user.username)
            }
            val maxSizes = list.map(_.map(_.length))
              .foldLeft(List(1, 1, 1, 1, 1)) { case (ls, entry) =>
              def greater (a: Int, b: Int) = if (a > b) a else b
              List(greater(ls.head, entry.head), greater(ls(1), entry(1)), greater(ls(2), entry(2)),
                greater(ls(3), entry(3)), greater(ls(4), entry(4)))
            }
            val formatted = list.map { entry =>
              def spaces(num: Int) = List.fill(num)(" ").mkString("")
              entry.zip(0 until 5) map { attrib =>
                val length = maxSizes.max - attrib._1.length
                val cush = spaces(length)
                val offset = cush.length % 2
                cush.drop(length / 2) + attrib._1 + cush.drop(length / 2 + offset)
              }
            }
            val horizontal = List.fill(maxSizes.max * maxSizes.size + 6)("-").mkString("")
            println(horizontal)
            formatted foreach (e => println("|" + e.mkString("|") + "|"))
            println(horizontal)
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

      case "save" =>
        val ret = await[Message](SaveRequest, driver)
        println(ret.contents)

      // Default and unknown case
      case _ => println("Unknown input, try again.")
    }) match {
      case Success(_) =>
      case Failure(ex) =>
        println("Failed parsing input.")
    }
  }
}
