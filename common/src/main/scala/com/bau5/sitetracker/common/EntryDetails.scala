package com.bau5.sitetracker.common

import org.joda.time.DateTime

/**
  * Created by Rick on 1/15/16.
  */
object EntryDetails {
  case class SSystem(name: String)
  case class User(username: String)

  case class AnomalyEntry(user: User, anomaly: Anomaly, timeRecorded: DateTime) {
    override def toString: String = {
      s"Entry[User:${user.username}, $anomaly, Time:$timeRecorded]"
    }

    def stringify: String = {
      s"${anomaly.stringify},${user.username},${timeRecorded.getMillis}"
    }
  }
  case class Anomaly(ident: Identifier, name: Name, typ: Type) {
    override def toString: String = {
      s"Anomaly[ID:${ident.id}, Name:'${name.name}', Type:${typ.str}]"
    }

    def stringify: String = {
      s"${ident.id},${name.name},${typ.str}"
    }
  }
  case class Identifier(id: String)
  case class Name(name: String)
  case class Type(str: String)
}
