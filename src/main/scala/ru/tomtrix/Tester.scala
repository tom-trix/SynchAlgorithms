package ru.tomtrix

import util.Random
import compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._

case class W(var i: Long)

object Tester extends App with IModel {
  var w = new W(0)

  def startModelling = {
    val rand = new Random(Platform.currentTime)
    system.scheduler.schedule(0 seconds, 300 milliseconds) {
      synchronized {
        logger debug s"time = $getTime, state = ${w.i}"
        addTime(1 + rand.nextInt(10))
        changeState {w.i += 1}
      }
      if (rand nextBoolean())
        sendMessageToAll(Some("trix"))
    }
    w
  }

  override def handleMessage(m: EventMessage) {
    super.handleMessage(m)
    logger info s"Yahoo! Принято сообщение от ${m.sender} с меткой ${m.t}"
    synchronized {
      logger debug s"time = $getTime, state = ${w.i}"
      changeState {w.i += 1}
    }
  }
}
