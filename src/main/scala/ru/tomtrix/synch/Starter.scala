package ru.tomtrix.synch

/** Dispatcher that informs all the other logic processes to start<br>This object can't receive the messages */
object Starter extends App with IModel[None.type] {
  def startModelling = None
  def onMessageReceived() {}
  while (true) {
    println("Press Enter to start...")
    readLine()
    sendMessageToAll(StartMessage)
  }
}
