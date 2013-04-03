package ru.tomtrix

import synch.OptimisticSynchronizator

/** Abstract trait that your model should implement */
trait IModel[T <: Serializable] extends Communicator[T] with OptimisticSynchronizator[T] {
  def startModelling: T

  private var time = 0d
  private var state: T = _

  def getTime = time

  def addTime(delta: Double) {
    synchronized {
      time += delta
    }
  }

  def getState = state

  def setState(s: T) {
    synchronized {
      state = s
    }
  }

  def changeState(f: T => Unit) {
    snapshot(time)
    synchronized {
      f(state)
    }
  }

  def sendMessage(whom: String, m: Message) {
    actors.get(whom) map {_ ! m} getOrElse logger.error(s"No such an actor: $whom")
  }

  def sendMessage(whom: String, text: String) {
    sendMessage(whom, InfoMessage(actorname, text))
  }

  def sendMessage(whom: String, data: Serializable) {
    sendMessage(whom, EventMessage(time, actorname, data))
  }

  def sendMessageToAll(m: Message) {
    actors foreach {_._2 ! m}
  }

  def sendMessageToAll(text: String) {
    sendMessageToAll(InfoMessage(actorname, text))
  }

  def sendMessageToAll(data: Serializable) {
    sendMessageToAll(EventMessage(time, actorname, data))
  }
}
