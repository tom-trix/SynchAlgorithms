package ru.tomtrix.synch

import ru.tomtrix.synch.algorithms.AgentEvent

/**
 * Dispatcher that informs all the other logic processes to start
 */
object Starter extends App with Model[Stub] {
  def startModelling = Stub(0)
  def convertRollback(m: EventMessage): AgentEvent = null
  while (true) {
    readLine().toLowerCase.trim match {
      case "stop" => sendMessageToAll(StopMessage)
      case _ =>      sendMessageToAll(StartMessage)
    }
  }
}
