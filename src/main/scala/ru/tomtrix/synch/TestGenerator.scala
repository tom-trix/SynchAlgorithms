package ru.tomtrix.synch

import scala.math._
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import org.apache.log4j.Logger
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.ModelObservable._
import ru.tomtrix.synch.algorithms.AgentEvent

/**
 * Case class that contains results of a single modelling launch
 * @param statistics map Category -> value
 * @param time time taken by the launch
 */
case class Results(statistics: Statistics, time: Long)

/**
 * Simple model state stub
 * @param n fake internal variable of a state
 */
case class Stub(var n: Long) extends HashSerializable {
  def toHash = n.toString
  /**
   * Method for Java (cause case class doesn't provide the setters)
   */
  def javaInc() {
    n+=1
  }
}

/**
 * Test generator that is intended to be a Starter: it runs the modelling several times and aggregates
 * the obtained statistics by average measure. Args(0) may comprise the count of runs (otherwise count = 100)
 */
object TestGenerator extends App with Model[Stub] {

  /** Count of launches (1 or more) */
  private val n = safe$ {max(args(0).toInt, 1)} getOrElse 100

  /** Buffer of all retrieved statistics (1..n) */
  private val totalStatistics = ArrayBuffer[Results]()

  /** Special barrier synchronizator that allows to run code once among <b>actor.size</b> threads */
  private val barrier = new BarrierSynch(actors.size)

  /** Current launch */
  private var i = 0

  /** Time taken by a current launch */
  private var time = -1L

  /** This buffer is used to send TimeRequest messages only if they're needed */
  private var nodes = ListBuffer[String]()

  /**
   * Constructor: triggers the scheduler that checks the modelling time, and starts the process
   */
  safe {
    system.scheduler.schedule(500 milliseconds, 1 second) {
      synchronized {
        nodes foreach {sendMessage(_, TimeRequest)}
      }
    }
    startModelling
  }

  def convertRollback(m: EventMessage): AgentEvent = null

  def startModelling = {
    nodes ++= actornames
    sendMessageToAll(StartMessage)
    time = Platform.currentTime
    i += 1
    Stub(0)
  }

  override def onReceive() = {
    case m: TimeResponse => checkTime(m)
    case m: StatResponse => addToStatistics(m)
    case _ => logger error "Unknown message"
  }

  override def stopModelling() = {
    var result: Statistics = Map.empty
    val logger = Logger getLogger "statistics"
    logger info s"Total tests: $i"
    logger info "======================="
    logger info s"Average time = ${totalStatistics.map{_.time}.sum / totalStatistics.size}"
    // считаем сумму всех показателей
    result = totalStatistics map {_.statistics} reduce((x, y) => x zip y map {t => t._1._1 -> (t._1._2+t._2._2)})
    // делим суммы на число тестов
    result = result map {t => t._1 -> t._2/totalStatistics.size}
    printStatistics(result)
    system shutdown()
    result
  }

  /**
   * Checks whether the time overcame the treshold and sends StopMessage if any
   * @param m TimeResponse with the timestamp
   */
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

  /**
   * Puts the statistics brought by message <b>m</b> into a buffer and restarts the process.<br>
   * Restarting is protected by barrier so that only the one thread can perform it
   * @param m StatResponse message
   */
  def addToStatistics(m: StatResponse) {
    safe {
      synchronized {
        log"statistics received ${m.statistics}"
        totalStatistics += Results(m.statistics, Platform.currentTime - time)
        barrier.runByLast {
          if (i < n) startModelling
          else stopModelling()
        }
      }
    }
  }
}
