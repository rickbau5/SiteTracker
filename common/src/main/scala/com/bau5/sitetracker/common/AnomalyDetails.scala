package com.bau5.sitetracker.common

import org.joda.time.DateTime

/**
  * Created by Rick on 1/15/16.
  */
object AnomalyDetails {
  trait AnomalyDetail

  case class SSystem(name: String) extends AnomalyDetail
  case class User(username: String) extends AnomalyDetail

  case class AnomalyEntry(user: User, anomaly: Anomaly, timeRecorded: DateTime) extends AnomalyDetail {
    def update(detail: AnomalyDetail): AnomalyEntry = detail match {
      case id: Identifier  => AnomalyEntry(user, Anomaly(id, anomaly.name, anomaly.typ), timeRecorded)
      case name: Name      => AnomalyEntry(user, Anomaly(anomaly.ident, name, anomaly.typ), timeRecorded)
      case typ: Type       => AnomalyEntry(user, Anomaly(anomaly.ident, anomaly.name, typ), timeRecorded)
    }
    override def toString: String = {
      s"Entry[User:${user.username}, $anomaly, Time:$timeRecorded]"
    }

    def stringify: String = {
      s"${anomaly.stringify},${user.username},${timeRecorded.getMillis}"
    }
  }
  case class Anomaly(ident: Identifier, name: Name, typ: Type)  extends AnomalyDetail {
    override def toString: String = {
      s"Anomaly[ID:${ident.id}, Name:'${name.name}', Type:${typ.str}]"
    }

    def stringify: String = {
      s"${ident.id},${name.name},${typ.str}"
    }
  }
  case class Identifier(id: String) extends AnomalyDetail
  case class Name(name: String) extends AnomalyDetail
  case class Type(str: String) extends AnomalyDetail
}
