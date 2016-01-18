package com.bau5.sitetracker.server

import akka.actor.PoisonPill
import akka.util.Timeout
import com.bau5.sitetracker.common.BaseProvider
import com.bau5.sitetracker.common.Events.SaveRequest

import scala.concurrent.duration._
import scala.io.StdIn

/**
  * Created by Rick on 1/15/16.
  */
object Main extends BaseProvider("ServerSystem", "") {
  override implicit val timeout: Timeout = Timeout(5 seconds)

  def main(args: Array[String]) {
    val serverActor = newActor[ServerActor]("server")
    while (true) StdIn.readLine("> ") match {
      case "save" =>
        serverActor ! SaveRequest()
      case "quit" =>
        serverActor ! SaveRequest()
        serverActor ! PoisonPill
        sys.exit(0)
      case _ =>
        println("Unrecognized input.")
    }
  }
}
