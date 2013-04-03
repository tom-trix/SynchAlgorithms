package ru.tomtrix

import synch.OptimisticSynchronizator

/** Abstract trait that your model should implement */
trait IModel extends Communicator with OptimisticSynchronizator {
  def startModelling: Serializable

  private var time = 0d
  private var state: Serializable = None

  def getTime = time

  def addTime(delta: Double) {
    synchronized {
      time += delta
    }
  }

  def getState = state

  def setState(s: Serializable) {
    synchronized {
      state = s
    }
  }

  def changeState(f: => Unit) {
    snapshot(time)
    synchronized {
      f
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
