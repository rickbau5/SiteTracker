package com.bau5.sitetracker.common

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
  * Created by Rick on 1/15/16.
  */
object AnomalyDetails {
  val formatter = DateTimeFormat.forPattern("HH:mm:ss MM/dd/yyyy")

  sealed trait AnomalyDetail

  case class SSystem(value: String) extends AnomalyDetail
  case class User(value: String) extends AnomalyDetail
  case class Time(value: DateTime) extends AnomalyDetail {
    override def toString: String = value.toString(formatter)
  }

  case class AnomalyEntry(user: User, anomaly: Anomaly, timeRecorded: Time) extends AnomalyDetail {
    def update(detail: AnomalyDetail): AnomalyEntry = detail match {
      case id: Identifier  => AnomalyEntry(user, Anomaly(id, anomaly.name, anomaly.typ), timeRecorded)
      case name: Name      => AnomalyEntry(user, Anomaly(anomaly.ident, name, anomaly.typ), timeRecorded)
      case typ: Type       => AnomalyEntry(user, Anomaly(anomaly.ident, anomaly.name, typ), timeRecorded)
      case _ => // Unsupported
        this
    }
    override def toString: String = {
      s"Entry[User:${user.value}, $anomaly, Time:$timeRecorded]"
    }

    def stringify: String = {
      s"${anomaly.stringify},${user.value},${timeRecorded.toString}"
    }
  }
  case class Anomaly(ident: Identifier, name: Name, typ: Type)  extends AnomalyDetail {
    override def toString: String = {
      s"Anomaly[ID:${ident.value}, Name:'${name.value}', Type:${typ.value}]"
    }

    def stringify: String = {
      s"${ident.value},${name.value},${typ.value}"
    }
  }
  case class Identifier(value: String) extends AnomalyDetail
  case class Name(value: String) extends AnomalyDetail
  case class Type(value: String) extends AnomalyDetail
}
