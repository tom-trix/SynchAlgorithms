package ru.tomtrix.synch

import scala.math._
import scala.collection.mutable
import org.apache.log4j.Logger
import ru.tomtrix.synch.ModelObservable._

object ModelObservable {
  type Statistics = Map[Category, Double]

  abstract sealed class Category extends Serializable {
    override def toString = getClass.getSimpleName.substring(0, getClass.getSimpleName.length-1)
  }

  object EVENTS_HANDLED extends Category
  object RECEIVED_MESSAGES extends Category
  object RECEIVED_EVENT_MESSAGES extends Category
  object RECEIVED_ANTI_MESSAGES extends Category
  object SENT_MESSAGES extends Category
  object SENT_EVENT_MESSAGES extends Category
  object SENT_ANTI_MESSAGES extends Category
  object ROLLBACKS extends Category
  object ROLLBACKS_MAXDEPTH extends Category
  object ROLLBACKS_DEPTH_1 extends Category
  object ROLLBACKS_DEPTH_2 extends Category
  object ROLLBACKS_DEPTH_3 extends Category
  object ROLLBACKS_DEPTH_MORE extends Category
  object MAX_TIME_WINDOW extends Category
}



/**
 * gr
 */
trait ModelObservable {
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
    ROLLBACKS_DEPTH_MORE -> 0d,
    MAX_TIME_WINDOW -> 0d
  )

  def stopModelling(): Statistics = {
    synchronized {
      val result = statistics.toMap
      printStatistics(result)
      statistics foreach {t => statistics(t._1) = 0}
      result
    }
  }

  def printStatistics(statistics: Statistics) {
    statistics.toList sortBy {_._1.toString} foreach {t =>
      Logger getLogger "statistics" info f"${t._1}%25s:   ${t._2}%4.1f"
    }
  }

  def statMessageSent(m: Message) {
    synchronized {
      statistics(SENT_MESSAGES) += 1
      m match {
        case _: EventMessage => statistics(SENT_EVENT_MESSAGES) += 1
        case _: AntiMessage => statistics(SENT_ANTI_MESSAGES) += 1
        case _ =>
      }
    }
  }

  def statMessageReceived(m: Message) {
    synchronized {
      statistics(RECEIVED_MESSAGES) += 1
      m match {
        case _: EventMessage => statistics(RECEIVED_EVENT_MESSAGES) += 1
        case _: AntiMessage => statistics(RECEIVED_ANTI_MESSAGES) += 1
        case _ =>
      }
    }
  }

  def statRolledback(depth: Int, timeWindow: Double) {
    synchronized {
      statistics(ROLLBACKS) += 1
      depth match {
        case 1 => statistics(ROLLBACKS_DEPTH_1) += 1
        case 2 => statistics(ROLLBACKS_DEPTH_2) += 1
        case 3 => statistics(ROLLBACKS_DEPTH_3) += 1
        case i if i > 3 => statistics(ROLLBACKS_DEPTH_MORE) += 1
        case _ =>
      }
      statistics(ROLLBACKS_MAXDEPTH) = max(statistics(ROLLBACKS_MAXDEPTH), depth)
      statistics(MAX_TIME_WINDOW) = max(statistics(MAX_TIME_WINDOW), round(timeWindow).toInt)
    }
  }
}
