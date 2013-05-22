package ru.tomtrix.synch

import scala.math._
import scala.collection.mutable
import org.apache.log4j.Logger
import ru.tomtrix.synch.ModelObservable._
import ru.tomtrix.synch.structures._

/**
 * Category is a single statistics parameter
 */
abstract sealed class Category extends Serializable {
  override def toString = getClass.getSimpleName.substring(0, getClass.getSimpleName.length-1)
}

/**
 * Companion for [[ru.tomtrix.synch.ModelObservable ModelObservable]]
 */
object ModelObservable {
  /** Special alias for <b>Map[Category, Double]</b> */
  type Statistics = Map[Category, Double]

  /** Total amount of events handled by a logic process */
  object EVENTS_HANDLED extends Category
  /** Total amount of messages received by a logic process */
  object RECEIVED_MESSAGES extends Category
  /** Amount of received event messages */
  object RECEIVED_EVENT_MESSAGES extends Category
  /** Amount of received anti-messages */
  object RECEIVED_ANTI_MESSAGES extends Category
  /** Total amount of messages sent by a logic process */
  object SENT_MESSAGES extends Category
  /** Amount of sent event messages */
  object SENT_EVENT_MESSAGES extends Category
  /** Amount of sent anti-messages */
  object SENT_ANTI_MESSAGES extends Category
  /** Total amount of rollbacks that a logic process incurred */
  object ROLLBACKS extends Category
  /** Max count of states rolled back during the modelling */
  object ROLLBACKS_MAXDEPTH extends Category
  /** Amount of rollbacks that turned the single state back */
  object ROLLBACKS_DEPTH_1 extends Category
  /** Amount of rollbacks that turned back 2 states */
  object ROLLBACKS_DEPTH_2 extends Category
  /** Amount of rollbacks that turned back 3 states */
  object ROLLBACKS_DEPTH_3 extends Category
  /** Amount of rollbacks that turned back 4 states */
  object ROLLBACKS_DEPTH_4 extends Category
  /** Amount of rollbacks that turned back 5 states */
  object ROLLBACKS_DEPTH_5 extends Category
  /** Amount of rollbacks that turned back 6 states */
  object ROLLBACKS_DEPTH_6 extends Category
  /** Amount of rollbacks that turned back 7 states */
  object ROLLBACKS_DEPTH_7 extends Category
  /** Amount of rollbacks that turned back 8 or more states */
  object ROLLBACKS_DEPTH_MORE extends Category
  /** Max model time duration that was rolled back during the modelling */
  object MAX_TIME_WINDOW extends Category
}


/**
 * Trait that adds the functionality of Information Procedures. It collects and aggregates the statistics
 */
trait ModelObservable {
  /** Main statistics map: Category -> value */
  private val statistics = mutable.HashMap(
    EVENTS_HANDLED -> 0d,
    RECEIVED_MESSAGES -> 0d,
    RECEIVED_EVENT_MESSAGES -> 0d,
    RECEIVED_ANTI_MESSAGES -> 0d,
    SENT_MESSAGES -> 0d,
    SENT_EVENT_MESSAGES -> 0d,
    SENT_ANTI_MESSAGES -> 0d,
    ROLLBACKS -> 0d,
    ROLLBACKS_MAXDEPTH -> 0d,
    ROLLBACKS_DEPTH_1 -> 0d,
    ROLLBACKS_DEPTH_2 -> 0d,
    ROLLBACKS_DEPTH_3 -> 0d,
    ROLLBACKS_DEPTH_4 -> 0d,
    ROLLBACKS_DEPTH_5 -> 0d,
    ROLLBACKS_DEPTH_6 -> 0d,
    ROLLBACKS_DEPTH_7 -> 0d,
    ROLLBACKS_DEPTH_MORE -> 0d,
    MAX_TIME_WINDOW -> 0d
  )

  /**
   * Writes all the gathered statistics into a log and flushs an internal buffer to get ready to restarting modelling
   * @return the gathered statistics
   */
  def stopModelling(): Statistics = {
    synchronized {
      val result = statistics.toMap
      printStatistics(result)
      statistics foreach {t => statistics(t._1) = 0}
      result
    }
  }

  /**
   * Writes statistics into a log.<br>
   * You can use any <b>Statistics</b> map
   * @param statistics any map: Category -> value
   */
  def printStatistics(statistics: Statistics) {
    statistics.toList sortBy {_._1.toString} foreach {t =>
      Logger getLogger "statistics" info f"${t._1}%25s:   ${t._2}%4.1f"
    }
  }

  /**
   * Captures information about sent messages
   * @param m message
   */
  def statMessageSent(m: BaseMessage) {
    synchronized {
      statistics(SENT_MESSAGES) += 1
      m match {
        case _: EventMessage => statistics(SENT_EVENT_MESSAGES) += 1
        case _: AntiMessage => statistics(SENT_ANTI_MESSAGES) += 1
        case _ =>
      }
    }
  }

  /**
   * Captures information about received messages
   * @param m message
   */
  def statMessageReceived(m: BaseMessage) {
    synchronized {
      statistics(RECEIVED_MESSAGES) += 1
      m match {
        case _: EventMessage => statistics(RECEIVED_EVENT_MESSAGES) += 1
        case _: AntiMessage => statistics(RECEIVED_ANTI_MESSAGES) += 1
        case _ =>
      }
    }
  }

  /**
   * Captures information about rollbacks
   * @param depth count of rolled back states
   * @param timeWindow model time duration
   */
  def statRolledback(depth: Int, timeWindow: Double) {
    synchronized {
      statistics(ROLLBACKS) += 1
      depth match {
        case 1 => statistics(ROLLBACKS_DEPTH_1) += 1
        case 2 => statistics(ROLLBACKS_DEPTH_2) += 1
        case 3 => statistics(ROLLBACKS_DEPTH_3) += 1
        case 4 => statistics(ROLLBACKS_DEPTH_4) += 1
        case 5 => statistics(ROLLBACKS_DEPTH_5) += 1
        case 6 => statistics(ROLLBACKS_DEPTH_6) += 1
        case 7 => statistics(ROLLBACKS_DEPTH_7) += 1
        case i if i >= 8 => statistics(ROLLBACKS_DEPTH_MORE) += 1
        case _ =>
      }
      statistics(ROLLBACKS_MAXDEPTH) = max(statistics(ROLLBACKS_MAXDEPTH), depth)
      statistics(MAX_TIME_WINDOW) = max(statistics(MAX_TIME_WINDOW), round(timeWindow).toInt)
    }
  }

  /**
   * Captures information about handled events
   */
  def statEventHandled() {
    synchronized {
      statistics(EVENTS_HANDLED) += 1
    }
  }
}
