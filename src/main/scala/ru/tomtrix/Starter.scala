package ru.tomtrix

/**
 * Dispatcher that after being run inform all the other logic processes to start<br>
 * This couldn't receive the messages
 */
object Starter extends App with IModel {
  val state: Serializable = None
  def startModelling() {}
  def getTime = 0
  while (true) {
    println("Press Enter to start...")
    readLine()
    sendMessageToAll(StartMessage)
  }
}
