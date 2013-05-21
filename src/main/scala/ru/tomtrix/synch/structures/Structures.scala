package ru.tomtrix.synch.structures

import java.util.UUID
import ru.tomtrix.synch.StringUtils._
import ru.tomtrix.synch.ModelObservable._

case class AgentEvent(agens: String, patiens: String, predicate: String) {
  var userdata: java.io.Serializable = None
  def withData(d: java.io.Serializable) = {this.userdata = d; this}
}

case class TimeEvent(t: Float, event: AgentEvent) extends Comparable[TimeEvent] {
  override def toString = s"${getClass.getSimpleName}(${t roundBy 2}, $event)"
  def compareTo(that: TimeEvent): Int = (math.signum(t - that.t)).asInstanceOf[Int]
}

abstract sealed class Message extends Serializable {
  val sender: String
}

abstract sealed class BaseMessage extends Message with Comparable[BaseMessage] {
  val id = UUID.randomUUID.toString

  def t: Float

  override def equals(obj: Any) = obj match {
    case m: BaseMessage => id == m.id
    case _ => false
  }

  def compareTo(that: BaseMessage): Int = (math.signum(t - that.t)).asInstanceOf[Int]
}

case class EventMessage(sender: String, timeevent: TimeEvent) extends BaseMessage {
  def t = timeevent.t
}

case class AntiMessage(sender: String, eventMessage: EventMessage) extends BaseMessage {
  override val id = eventMessage.id
  def t = eventMessage.timeevent.t
}

object StartMessage extends Message {
  val sender = ""
}

object StopMessage extends Message {
  val sender = ""
}

case class TimeRequest(sender: String) extends Message

case class TimeResponse(sender: String, t: Float) extends Message

case class LockRequest(sender: String) extends Message

case class LockResponse(sender: String) extends Message

case class StatResponse(sender: String, statistics: Statistics) extends Message