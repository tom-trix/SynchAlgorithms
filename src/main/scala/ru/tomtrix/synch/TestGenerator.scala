package ru.tomtrix.synch

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: Tom
 * Date: 08.04.13
 * Time: 19:28
 */
object TestGenerator extends App with IModel[None.type] {

  var nodes = ListBuffer[String]()

  var totalStatistics = mutable.HashMap[String, ArrayBuffer[Map[Category, Int]]]()

  def startModelling = None

  def onMessageReceived() {}

  workers foreach {t => new ProcessBuilder("java", "-jar", t).start()}
  Thread sleep 1000
  system.scheduler.schedule(0 minutes, 500 milliseconds) {
    synchronized {
      nodes foreach {sendMessage(_, TimeRequest(actorname))}
    }
  }
  tryToRestart()

  override def onReceive() = {
    case m: TimeResponse => checkTime(m)
    case m: StatResponse => addToStatistics(m)
    case _ =>
  }

  def checkTime(m: TimeResponse) {
    if (m.t > 1440)
      sendMessage(m.sender, StopMessage(actorname))
  }

  def addToStatistics(m: StatResponse) {
    synchronized {
      totalStatistics(m.sender) += m.statistics
      nodes -= m.sender
      tryToRestart()
    }
  }

  def tryToRestart() {
    synchronized {
      if (nodes.isEmpty) {
        nodes ++= actornames
        sendMessageToAll(StartMessage)
      }
    }
  }
}
