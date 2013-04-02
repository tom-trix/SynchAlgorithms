package ru.tomtrix

import java.util.concurrent.ConcurrentLinkedDeque
import Utils._

/** Abstract trait that your model should implement */
trait IModel extends Loggable {
  val state: Serializable

  def startModelling()
  def getTime: Double

  private val stack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()
  val communicator = new Communicator(this)

  def handleMessage(m: EventMessage) {
    synchronized {
      stack push m.t -> serialize(state)
      if (stack.size() > 10000) {
        stack.pollLast()
        logger error "stack overflown"
      }
    }
  }

  def sendMessage(whom: String, m: Message) {
    communicator.actors.get(whom) map {_ ! m} getOrElse logger.error(s"No such an actor: $whom")
  }

  def sendMessage(whom: String, text: String) {
    sendMessage(whom, InfoMessage(communicator.actorname, text))
  }

  def sendMessage(whom: String, data: Serializable) {
    sendMessage(whom, EventMessage(getTime, communicator.actorname, data))
  }

  def sendMessageToAll(m: Message) {
    communicator.actors foreach {_._2 ! m}
  }

  def sendMessageToAll(text: String) {
    sendMessageToAll(InfoMessage(communicator.actorname, text))
  }

  def sendMessageToAll(data: Serializable) {
    sendMessageToAll(EventMessage(getTime, communicator.actorname, data))
  }
}
