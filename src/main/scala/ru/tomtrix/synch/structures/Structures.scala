package ru.tomtrix.synch.structures

import java.util.UUID
import ru.tomtrix.synch.StringUtils._
import ru.tomtrix.synch.ModelObservable._

/**
 * The general semantic event construction
 * @param agens agent that operates the action
 * @param patiens agent that is operated on
 * @param predicate action
 */
case class AgentEvent(agens: String, patiens: String, predicate: String) {
  var userdata: java.io.Serializable = None
  def withData(d: java.io.Serializable) = {this.userdata = d; this}
}

/**
 * TimeEvent = AgentEvent + timestamp
 * @param t timestamp
 * @param event agent event
 */
case class TimeEvent(t: Float, event: AgentEvent) extends Comparable[TimeEvent] {
  override def toString = s"${getClass.getSimpleName}(${t roundBy 2}, $event)"
  def compareTo(that: TimeEvent): Int = (math.signum(t - that.t)).asInstanceOf[Int]
}

/**
 * The superclass for all the messages transmitted
 */
abstract sealed class Message extends Serializable {
  val sender: String
}

/**
 * Base message (that is common for Agent-Based Simulation on the whole)
 */
abstract sealed class BaseMessage extends Message with Comparable[BaseMessage] {
  val id = UUID.randomUUID.toString

  def t: Float

  override def equals(obj: Any) = obj match {
    case m: BaseMessage => id == m.id
    case _ => false
  }

  def compareTo(that: BaseMessage): Int = (math.signum(t - that.t)).asInstanceOf[Int]
}

/**
 * Message containing a time-event
 * @param sender sender
 * @param timeevent time-event
 */
case class EventMessage(sender: String, timeevent: TimeEvent) extends BaseMessage {
  def t = timeevent.t
}

/**
 * Message that used to manage rollbacks
 * @param sender sender
 * @param eventMessage message to be eliminated
 */
case class AntiMessage(sender: String, eventMessage: EventMessage) extends BaseMessage {
  override val id = eventMessage.id
  def t = eventMessage.timeevent.t
}

/**
 * Message to start up the modelling process
 * <br>Usually is send by a starter
 */
object StartMessage extends Message {
  val sender = ""
}

/**
 * Message to stop the modelling process
 * <br>Usually is send by a starter
 * @param sender sender
 */
case class StopMessage(sender: String) extends Message

/**
 * Message to ask for a local time of the other process
 * <br>Usually is send by a starter to deside whether the simulation should be stopped
 * @param sender sender
 */
case class TimeRequest(sender: String) extends Message

/**
 * Message to response on a [[ru.tomtrix.synch.structures.TimeRequest TimeRequest]]
 * @param sender sender
 * @param t local time
 */
case class TimeResponse(sender: String, t: Float) extends Message

/**
 * Message to ask the other process whether it is locked pending the current process
 * <br> By the conversion if it is pending then it <b>MUST</b> return [[ru.tomtrix.synch.structures.LockResponse LockResponse]] message
 * @param sender sender
 */
case class LockRequest(sender: String) extends Message

/**
 * Message to response on a [[ru.tomtrix.synch.structures.LockRequest LockRequest]]
 * @param sender sender
 */
case class LockResponse(sender: String) extends Message

/**
 * Message with the gathered statistics
 * @param sender sender
 * @param statistics map {Category -> Number}
 */
case class StatResponse(sender: String, statistics: Statistics) extends Message