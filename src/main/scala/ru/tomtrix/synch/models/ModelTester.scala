package ru.tomtrix.synch.models

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import ru.tomtrix.synch._
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.algorithms.AgentEvent

/**
 * Date: 18.05.13
 */
object ModelTester extends App with Model[Stub] {
  def convertToEvent(m: EventMessage): AgentEvent = null
  def convertToActor(e: AgentEvent): String = ""
  def suspendModelling() {}
  def resumeModelling() {}
  def startModelling = Stub(0)

  var barrier = new BarrierSynch(actors.size)
  var stopped = false

  safe {
    log"Hello! Press Enter to start!"
    readLine()
    system.scheduler.schedule(500 milliseconds, 3 second) {
      synchronized {
        sendMessageToAll(TimeRequest)
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
