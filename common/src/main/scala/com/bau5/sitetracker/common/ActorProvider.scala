package com.bau5.sitetracker.common

import java.io.File

import akka.actor.{Props, ActorRef, Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.bau5.sitetracker.common.Events.{Request, Response}
import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.{Await, ExecutionContext}
import scala.reflect.ClassTag

/**
  * Created by Rick on 1/15/16.
  */
abstract class BaseProvider(systemName: String,
                            configName: String) extends SystemProvider with ActorProvider {
  implicit val actorSystem = initSystem(systemName, loadConfig(configName))

  def loadConfig(configName: String): Config = configName match {
    case "" => ConfigFactory.load()
    case  _ =>
      ConfigFactory.parseFile(
        new File(
          getClass.getClassLoader.getResource(configName).getFile
        )
      )
  }

  def initSystem(systemName: String, config: Config): ActorSystem = ActorSystem(systemName, config)
}

trait SystemProvider {
  implicit val actorSystem: ActorSystem
  lazy implicit val executionContext = actorSystem.dispatcher

  def initSystem(systemName: String, config: Config): ActorSystem
}

trait ActorProvider {
  implicit val timeout: Timeout
  implicit val executionContext: ExecutionContext

  def newActor[T <: Actor](actorName: String)(implicit ct: ClassTag[T], actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(Props[T], actorName)

  def await[T <: Response](request: Request, actor: ActorRef)(implicit ct: ClassTag[T]): T = {
    Await.result(
      for (response <- (actor ? request).mapTo[T]) yield response,
      timeout.duration
    )
  }
}
