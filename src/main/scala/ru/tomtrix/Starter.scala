package ru.tomtrix

/**
 * Dispatcher that after being run informs all the other logic processes to start<br>
 * This couldn't receive the messages
 */
object Starter extends App with IModel {
  var state: Serializable = None
  def startModelling() {}
  while (true) {
    println("Press Enter to start...")
    readLine()
    sendMessageToAll(StartMessage)
  }
}
