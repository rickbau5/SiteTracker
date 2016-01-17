package com.bau5.sitetracker.server

import com.bau5.sitetracker.common.BaseProvider

/**
  * Created by Rick on 1/15/16.
  */
object Main extends BaseProvider {
  def main(args: Array[String]) {
    val serverConfig = loadConfig("server")
    val serverSystem = initSystem("ServerSystem", serverConfig)

    val serverActor = newActor[SiteTrackerServerActor](serverSystem, "server")
  }
}
