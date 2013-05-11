package ru.tomtrix.synch

/**
 * Dispatcher that informs all the other logic processes to start
 */
object Starter extends App with Model[Stub] {
  def startModelling = Stub(0)
  while (true) {
    readLine().toLowerCase.trim match {
      case "stop" => sendMessageToAll(StopMessage)
      case _ =>      sendMessageToAll(StartMessage)
    }
  }
}
