package com.bau5.sitetracker.common

import com.bau5.sitetracker.common.EntryDetails.{AnomalyEntry, Identifier, SSystem, User}

/**
  * Created by Rick on 1/15/16.
  */
object Events {
  sealed trait Event
  sealed trait Request extends Event
  sealed trait Response extends Event

  sealed trait ServerRequest extends Request

  sealed trait Mutator

  case class Connect() extends ServerRequest
  case class ConnectionSuccessful() extends Response

  case class Login(user: User) extends ServerRequest
  case class Logout(user: User) extends ServerRequest

  case class Message(contents: String) extends Response
  case class ServerMessage(contents: String) extends ServerRequest

  case class SeeSystemRequest(system: String) extends ServerRequest
  case class SeeSystemResponse(entries: Option[List[AnomalyEntry]]) extends Response

  case class ListSystemsRequest() extends ServerRequest
  case class ListSystemsResponse(systems: List[(SSystem, Int)]) extends Response

  case class AddEntryRequest(entry: (SSystem, AnomalyEntry)) extends ServerRequest with Mutator
  case class AddEntryResponse(wasSuccessful: Boolean) extends Response

  case class RemoveEntryRequest(entry: (SSystem, Identifier)) extends ServerRequest with Mutator
  case class RemoveEntryResponse(wasSuccessful: Boolean) extends Response

  case class RemoveAllEntriesRequest(system: SSystem) extends ServerRequest with Mutator
  case class RemoveAllEntriesResponse(wasSuccessful: Boolean) extends Response

  case class DownloadEntriesRequest() extends ServerRequest
  case class DownloadEntriesResponse(entries: List[String]) extends Response

  case class SaveRequest() extends ServerRequest
}
