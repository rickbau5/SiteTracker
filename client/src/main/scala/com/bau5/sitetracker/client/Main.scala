package com.bau5.sitetracker.client

import scala.io.StdIn


/**
  * Created by Rick on 1/15/16.
  */
object Main {
  def main(args: Array[String]): Unit = {
    if (args.headOption.isDefined && args.head == "no-gui") {
      val commandLineClient = new Client().init()
      while (true) commandLineClient.handleInput(StdIn.readLine("> "))
    } else {
      val client = new Client().init()
      val gui = new SiteTrackerGui(client).main(args)
    }
  }
}
