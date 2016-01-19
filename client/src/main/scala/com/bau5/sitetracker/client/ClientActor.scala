package com.bau5.sitetracker.client

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.bau5.sitetracker.common.Events._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * Created by Rick on 1/16/16.
  */
class ClientActor extends Actor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)
  implicit val executionContext = context.dispatcher

  var remoteActorSelection: ActorSelection = _

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val host = context.system.settings.config.getString("server.host")
    remoteActorSelection = context.actorSelection(s"akka.tcp://ServerSystem@$host:5150/user/server")
  }

  override def receive: Receive = {
    case Message(msg) =>
      println(s"Driver got [$msg]")
    case serverRequest: ServerRequest =>
      val ret = await[Response](serverRequest, remoteActorSelection)
      sender ! ret
    case req =>
      println("Unhandled request type! " + req)
  }

  def await[T <: Response](request: Request, actor: ActorSelection)(implicit ct: ClassTag[T]): T = {
    Await.result(
      for (response <- (actor ? request).mapTo[T]) yield response,
      timeout.duration
    )
  }
}
