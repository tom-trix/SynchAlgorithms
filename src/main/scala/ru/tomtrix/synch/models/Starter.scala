package ru.tomtrix.synch.models

import ru.tomtrix.synch._
import ru.tomtrix.synch.structures._

/**
 * Dispatcher that informs all the other logic processes to start
 */
object Starter extends App with Model[Stub] {
  def startModelling = Stub(0)
  def suspendModelling(suspend: Boolean) {}
  def simulateStep(e: TimeEvent): Array[TimeEvent] = Array()

  while (true) {
    readLine().toLowerCase.trim match {
      case "stop" => sendMessageToAll(StopMessage)
      case _ =>      sendMessageToAll(StartMessage)
    }
  }
}
