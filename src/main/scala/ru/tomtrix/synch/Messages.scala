package ru.tomtrix.synch

import java.util.UUID
import ru.tomtrix.synch.ModelObservable._
import ru.tomtrix.synch.StringUtils._

/**
 * Base Message class to communicate among the actors.<br>
 * It contains a special generated ID and overriden {@link java.lang.Object#equals equals} method that compares by this ID
 */
sealed abstract class Message extends Serializable with Comparable[Message] {
  val t: Double
  val sender: String

  val id = UUID.randomUUID().toString

  override def equals(obj: Any) = obj match {
    case m: Message => id == m.id
    case _ => false
  }

  override def toString = {
    s"${getClass.getSimpleName}(${t roundBy 3}, $sender)"
  }

  def compareTo(that: Message): Int = (t - that.t).toInt
}

/**
 * Empty message that is usually used in a {@link scala.Option#getOrElse} clause
 */
object EmptyMessage extends Message {
  val t = -1d
  val sender = ""
}

/**
 * Message used exclusively by a Starter (to inform the others to start modelling)
 */
object StartMessage extends Message {
  val t = -1d
  val sender = "Starter"
}

/**
 * Message used exclusively by a Starter (to inform the others to stop modelling)
 */
object StopMessage extends Message {
  val t = -1d
  val sender = "Starter"
}

/**
 * Message used by a Starter to request the others' local time
 */
object TimeRequest extends Message {
  val t = -1d
  val sender = "Starter"
}

/**
 * Information message used basicly for debugging
 * @param sender name of actor sending the message
 * @param text message body
 */
case class InfoMessage(sender: String, text: String) extends Message {
  val t = -1d
}

/**
 * Message informing about the local time (usually sent as a response to a <b>ru.tomtrix.synch.TimeRequest</b> message)
 * @param t timestamp
 * @param sender name of actor sending the message
 */
case class TimeResponse(t: Double, sender: String) extends Message

/**
 * Message informing about the current statistics gathered about the logic process (usually sent as a response to a <b>StopModelling</b> message)
 * @param t timestamp
 * @param sender name of actor sending the message
 * @param statistics map: Category -> value
 */
case class StatResponse(t: Double, sender: String, statistics: Statistics) extends Message {
  override def toString = {
    val msg = super.toString
    s"${msg.substring(0, msg.length-1)}; $statistics)"
  }
}

/**
 * Main message that brings the model event
 * @param t timestamp
 * @param sender name of actor sending the message
 * @param data message body
 */
case class EventMessage(t: Double, sender: String, data: HashSerializable) extends Message {
  override def toString = {
    val msg = super.toString
    s"${msg.substring(0, msg.length-1)}; $data)"
  }
}

/**
 * Antimessage that is used to eliminate <b>EventMessages</b> sent erroneously before
 * @param baseMsg corresponding positive message
 */
class AntiMessage(baseMsg: Message) extends Message {
  val t = baseMsg.t
  val sender = baseMsg.sender
  override val id = baseMsg.id

  override def toString = {
    s"AntiMessage(${t roundBy 3})"
  }
}

/**
 * Classes used by Model's implicit methods
 */
object MessageImplicits {
  object TIME_RESPONSE
  case class INFO_MESSAGE(text: String)
  case class STAT_RESPONSE(stat: Statistics)
  case class EVENT_MESSAGE(data: HashSerializable)
}