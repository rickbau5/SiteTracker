package com.bau5.sitetracker.server

import akka.actor.PoisonPill
import akka.util.Timeout
import com.bau5.sitetracker.common.BaseProvider
import com.bau5.sitetracker.common.Events.{Message, MessageAll, SaveRequest}

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Created by Rick on 1/15/16.
  */
object Main extends BaseProvider("ServerSystem", "") {
  override implicit val timeout: Timeout = Timeout(5 seconds)

  val messageAll = "message-all (.+)".r

  def main(args: Array[String]) {
    val serverActor = newActor[ServerActor]("server")
    serverActor ! LoadSavedData

    while (true) StdIn.readLine("> ") match {
      case "save" =>
        serverActor ! SaveRequest
      case "quit" =>
        serverActor ! SaveRequest
        serverActor ! PoisonPill
        sys.exit(0)
      case messageAll(msg) =>
        serverActor ! MessageAll(Message(msg))
      case _ =>
        println("Unrecognized input.")
    }
  }
}
