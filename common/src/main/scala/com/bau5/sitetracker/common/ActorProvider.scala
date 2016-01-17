package com.bau5.sitetracker.common

import java.io.File

import akka.actor.{Props, ActorRef, Actor, ActorSystem}
import com.typesafe.config.{ConfigFactory, Config}

import scala.reflect.ClassTag

/**
  * Created by Rick on 1/15/16.
  */
class BaseProvider extends SystemProvider with ActorProvider {
  def loadConfig(side: String): Config = ConfigFactory.parseFile(
    new File(
      getClass.getClassLoader.getResource(s"${side.toLowerCase}_application.conf").getFile
    )
  )

  def initSystem(systemName: String, config: Config): ActorSystem = ActorSystem(systemName, config)
}

trait SystemProvider {
  def initSystem(systemName: String, config: Config): ActorSystem
}

trait ActorProvider {
  def newActor[T <: Actor](system: ActorSystem, actorName: String)(implicit ct: ClassTag[T]): ActorRef = system.actorOf(Props[T], actorName)
}
