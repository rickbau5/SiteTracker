package com.bau5.sitetracker.common

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.bau5.sitetracker.common.Events.{Request, Response}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Created by Rick on 1/16/16.
  */
object Utils {
  def await[T <: Response](req: Future[Response])(implicit timeout: Timeout) = Await.result(req, timeout.duration).asInstanceOf[T]

  def request[T <: Request](req: T, actor: ActorRef, ec: ExecutionContext)(implicit ct: ClassTag[T], timeout: Timeout): Future[Response] = {
    implicit val executionContext = ec
    for (response <- (actor ? req).mapTo[Response]) yield response
  }
}
