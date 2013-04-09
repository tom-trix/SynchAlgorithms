package ru.tomtrix.synch

import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import org.apache.log4j.Logger
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.ModelObservable._

case class Results(node: String, statistics: Statistics, time: Long)

/**
 * Created with IntelliJ IDEA.
 * User: Tom
 * Date: 08.04.13
 * Time: 19:28
 */
object TestGenerator extends App with IModel[None.type] {

  private var nodes = ListBuffer[String]()

  private var i = 0

  private val totalStatistics = ArrayBuffer[Results]()

  private var time = -1L

  private val n = safe$ {args(0).toInt} getOrElse 100

  def startModelling = None

  override def stopModelling() = {
    synchronized {
      system shutdown()
      var result: Statistics = Map.empty
      if (i > 0) {
        val logger = Logger getLogger "statistics"
        logger info s"Total tests: $i"
        logger info "======================="
        logger info s"Average time = ${totalStatistics.map{_.time}.sum / i}"
        // считаем сумму всех показателей
        result = totalStatistics map {_.statistics} reduce((x, y) => x zip y map {t => t._1._1 -> (t._1._2+t._2._2)})
        // делим суммы на число тестов
        result = result map {t => t._1 -> t._2/i}
        printStatistics(result)
      }
      result
    }
  }

  def onMessageReceived() {}

  safe {
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
        if (m.t > 440 && nodes.contains(m.sender)) {
          nodes -= m.sender
          sendMessage(m.sender, StopMessage)
        }
      }
    }
  }

  def addToStatistics(m: StatResponse) {
    safe {
      synchronized {
        totalStatistics += Results(m.sender, m.statistics, Platform.currentTime - time)
        log"total statistics = $totalStatistics"
        tryToRestart()
      }
    }
  }

  def tryToRestart() {
    safe {
      synchronized {
        log"trying to restart..."
        if (nodes.isEmpty)
          if (i < n) {
            nodes ++= actornames
            sendMessageToAll(StartMessage)
            time = Platform.currentTime
            i += 1
          } else stopModelling()
      }
    }
  }
}
