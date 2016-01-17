package com.bau5.sitetracker.client

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.bau5.sitetracker.common.Events._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Created by Rick on 1/16/16.
  */
class ClientDriverActor extends Actor with ActorLogging {
  implicit val timeout = Timeout(5 seconds)
  var remoteActor: ActorSelection = _

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    remoteActor = context.actorSelection("akka.tcp://ServerSystem@127.0.0.1:5150/user/server")
  }

  override def receive: Receive = {
    case Message(msg) =>
      println(s"Driver got [$msg]")
    case serverRequest: ServerRequest =>
      val ret = await(request(serverRequest, remoteActor, context.dispatcher))
      sender ! ret
    case req =>
      println("Unhandled request type! " + req)
  }

  def await(req: Future[Response])(implicit timeout: Timeout) = Await.result(req, timeout.duration)

  def request[T <: Request](req: T, actor: ActorSelection, ec: ExecutionContext)(implicit ct: ClassTag[T]): Future[Response] = {
    implicit val executionContext = ec
    for (response <- (actor ? req).mapTo[Response]) yield response
  }
}
