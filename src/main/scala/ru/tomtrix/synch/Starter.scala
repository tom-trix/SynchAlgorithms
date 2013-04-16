package ru.tomtrix.synch

/**
 * Dispatcher that informs all the other logic processes to start
 */
object Starter extends App with Model[Stub] {
  def startModelling = Stub(0)
  while (true) {
    println("Press Enter to start...")
    readLine()
    sendMessageToAll(StartMessage)
    println("Press Enter to stop...")
    readLine()
    sendMessageToAll(StopMessage)
  }
}
