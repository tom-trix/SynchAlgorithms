package ru.tomtrix

/**
 * Dispatcher that after being run inform all the other logic processes to start<br>
 * This couldn't receive the messages
 */
object Starter extends App {
  val communicator = new Communicator(new ModelLoadable {
    def startModelling() {}
  })
  communicator.actors foreach {_ ! StartMessage}
}
