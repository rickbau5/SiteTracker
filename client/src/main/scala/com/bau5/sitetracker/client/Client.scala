package com.bau5.sitetracker.client

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

import akka.actor.{Props, PoisonPill}
import akka.util.Timeout
import com.bau5.sitetracker.common.AnomalyDetails._
import com.bau5.sitetracker.common.Events._
import com.bau5.sitetracker.common.BaseProvider
import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

/**
  * Created by Rick on 1/17/16.
  */
class Client(systemName: String = "ClientSystem",
             configName: String = "") extends BaseProvider(systemName, configName) {
  implicit val timeout = Timeout(5 seconds)

  val idFormat = "(\\w{3}-\\d{3})"
  val seeSystem = "see (.+)".r
  val addEntry = s"add (.+) $idFormat ('.+') (.+)".r
  val editEntry = s"edit (.+) $idFormat \\{(.+)\\}".r
  val removeOneEntry = s"remove (.+) $idFormat".r
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

      // List all logged entries
      case "see all" =>
        val response = await[SeeAllSystemsResponse](SeeAllSystemsRequest, driver)
        response.entries match {
          case None => println("None")
          case Some(entries) =>
            entries foreach { case (k, v) =>
              println(k.name)
              formatAndPrint(v)
            }
        }

      // Lookup the entries in a system.
      case seeSystem(sys) =>
        val response = await[SeeSystemResponse](SeeSystemRequest(sys), driver)
        response.entries match {
          case None => println("None")
          case Some(entries) =>
            formatAndPrint(entries)
        }


      // List all systems
      case "list" =>
        val response = await[ListSystemsResponse](ListSystemsRequest, driver)
        println(response.systems.map(e => s" :${e._1.name} - ${e._2}").mkString("\n"))


      // Add a new entry
      case addEntry(system, id, name, typ) =>
        val newEntry = SSystem(system.head.toUpper + system.tail) -> AnomalyEntry(
          user,
          Anomaly(
            Identifier(id),
            Name(name.drop(1).dropRight(1)),
            Type(typ)
          ),
          Time(DateTime.now())
        )
        await[AddEntryResponse](AddEntryRequest(newEntry), driver)
          .wasSuccessful match {
            case true => println(s"+${newEntry._1.name} ${newEntry._2}")
            case false => println("Entry was not added.")
          }

      // Edit an entry
      case editEntry(system, id, attributes) =>
        //edit Tamo BCS-123 { type = Combat, name = Guristas Vantage Point, id = BNV-123, system = Test System }
        var buf = mutable.ListBuffer.empty[AnomalyDetail]

        val tups = attributes.split(",")      // fold?
          .map { e =>
            val r = e.split("=")
            (r.head.trim, r(1).trim)
          }.foreach {
            case (("system", syst)) => buf += SSystem(syst)
            case ((  "name", name)) => buf += Name(name)
            case ((  "type", typ))  => buf += Type(typ)
            case ((    "id", idt))  => buf += Identifier(idFormat.r.findFirstIn(idt).get)  // Unsafe, fail if bad entry.
          }

        await[EditEntryResponse](
          EditEntryRequest(
            (SSystem(system), Identifier(id)),
            buf.toList
          ), driver
        ).updatedEntry match {
          case Some(ret) => println(s"Successfully updated to [$ret]")
          case None => println("Nothing changed.")
        }

      // Remove an entry
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
        ex.printStackTrace()
        println("Failed parsing input.")
    }
  }

  def formatAndPrint(entries: List[AnomalyEntry]) = {
    val formatted = formatEntryList(entries)
    val horizontal = List.fill(formatted.head.length + 2)("-").mkString   // Make the horizontal bar for top & bottom
    println(horizontal)
    formatted foreach (e => println("|" + e + "|"))       // Print out tables with nice formatting
    println(horizontal)
  }

  def formatEntryList(entries: List[AnomalyEntry]): List[String] = {
    val list = entries map { entry =>           // Stringify the entries
      List(entry.anomaly.ident.id, entry.anomaly.name.name, entry.anomaly.typ.str,
        entry.timeRecorded.toString, entry.user.username)
    }
    val maxSizes = list.map(_.map(_.length))    // Calculate the max box size
      .foldLeft(List(1, 1, 1, 1, 1)) { case (ls, entry) =>
        List(ls.head.max(entry.head), ls(1).max(entry(1)), ls(2).max(entry(2)),
          ls(3).max(entry(3)), ls(4).max(entry(4)))
      }
    list.map { entry =>                         // Cushion the boxes so they are all the same size
      def spaces(num: Int) = List.fill(num)(" ").mkString("")
      entry.zip(0 until 5).map { attrib =>
        val length = maxSizes(attrib._2) - attrib._1.length + 2
        val cush = spaces(length)
        val offset = cush.length % 2
        cush.drop(length / 2) + attrib._1 + cush.drop(length / 2 + offset)
      }.mkString("|")
    }
  }
}
