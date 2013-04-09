package ru.tomtrix.synch

import java.util.UUID
import ModelObservable._

/** Base Message class to communicate among the actors */
sealed abstract class Message extends Serializable {
  val t: Double
  val sender: String

  val id = UUID.randomUUID().toString

  override def equals(obj: Any) = obj match {
    case m: Message => id == m.id
    case _ => false
  }
}

object EmptyMessage extends Message {
  val t = -1d
  val sender = ""
}

/** Message exclusively for Starter (to inform the others to start modelling) */
object StartMessage extends Message {
  val t = -1d
  val sender = "Starter"
}

object TimeRequest extends Message {
  val t = -1d
  val sender = "Starter"
}

object StopMessage extends Message {
  val t = -1d
  val sender = "Starter"
}

/**
 * Information message used basicly for debugging
 * @param sender name of actor dending the message
 * @param text message body
 */
case class InfoMessage(sender: String, text: String) extends Message {
  val t = -1d
}

case class TimeResponse(t: Double, sender: String) extends Message

case class StatResponse(t: Double, sender: String, statistics: Statistics) extends Message

/**
 * Main message that brings the model event
 * @param t timestamp
 * @param sender name of actor dending the message
 * @param data message body
 */
case class EventMessage(t: Double, sender: String, data: Serializable) extends Message

class AntiMessage(baseMsg: Message) extends Message {
  val t = baseMsg.t
  val sender = baseMsg.sender
  override val id = baseMsg.id

  override def toString = {
    s"AntiMessage (t = $t)"
  }
}

object MessageImplicits {
  object TIME_RESPONSE
  case class INFO_MESSAGE(text: String)
  case class STAT_RESPONSE(stat: Statistics)
  case class EVENT_MESSAGE(data: Serializable)
}