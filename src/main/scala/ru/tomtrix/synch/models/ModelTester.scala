package ru.tomtrix.synch.models

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import ru.tomtrix.synch._
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.structures._

/**
 * Date: 18.05.13
 */
object ModelTester extends App with Model[Stub] {
  def suspendModelling(suspend: Boolean) {}
  def simulateStep(e: TimeEvent): Array[TimeEvent] = Array()
  def startModelling = Stub(0)

  var barrier = new BarrierSynch(actors.size)
  var stopped = false

  safe {
    log"Hello! Press Enter to start!"
    readLine()
    system.scheduler.schedule(500 milliseconds, 3 second) {
      synchronized {
        sendMessageToAll(TimeRequest(actorname))
      }
    }
    sendMessageToAll(StartMessage)
  }

  override def onReceive() = {
    case m: TimeResponse => if (!stopped && m.t > 300) {
      sendMessageToAll(StopMessage)
      stopped = true
    }
    case m: StatResponse => check()
  }

  private def check() = synchronized {
    barrier.runByLast {
      log"RESTART!!!"
      sendMessageToAll(StartMessage)
      stopped = false
    }
  }
}
