package ru.tomtrix.synch

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.mutable
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.SafeCode._

/**
 * Created with IntelliJ IDEA.
 * User: Tom
 * Date: 08.04.13
 * Time: 19:28
 */
object TestGenerator extends App with IModel[None.type] {

  private var nodes = ListBuffer[String]()

  private val totalStatistics = mutable.HashMap[String, ArrayBuffer[Map[Category, Int]]]()

  def startModelling = None

  def onMessageReceived() {}

  safe {
    actornames foreach {totalStatistics += _ -> ArrayBuffer()}
    system.scheduler.schedule(500 milliseconds, 1 second) {
      synchronized {
        nodes foreach {sendMessage(_, TimeRequest)}
      }
    }
    tryToRestart()
  }

  override def onReceive() = {
    case m: TimeResponse => checkTime(m)
    case m: StatResponse => addToStatistics(m)
    case _ => logger error "Unknown message"
  }

  def checkTime(m: TimeResponse) {
    safe {
      synchronized {
        log"t = ${m.t}"
        if (m.t > 1440 && nodes.contains(m.sender)) {
          nodes -= m.sender
          sendMessage(m.sender, StopMessage)
        }
      }
    }
  }

  def addToStatistics(m: StatResponse) {
    safe {
      synchronized {
        log"statistics = ${m.statistics}"
        totalStatistics(m.sender) += m.statistics
        log"total statistics = $totalStatistics"
        tryToRestart()
      }
    }
  }

  def tryToRestart() {
    safe {
      synchronized {
        log"trying to restart..."
        if (nodes.isEmpty) {
          nodes ++= actornames
          sendMessageToAll(StartMessage)
        }
      }
    }
  }
}
