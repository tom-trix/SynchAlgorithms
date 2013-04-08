package ru.tomtrix.synch

import ru.tomtrix.synch.algorithms.OptimisticSynchronizator

/** Abstract trait that your model should implement */
trait IModel[T <: Serializable] extends Communicator[T] with OptimisticSynchronizator[T] {

  /**
   * The basic method you must implement. It'll be invoked as soon as the Starter sends a message to get started.
   * Your model ought to contain a state (any object you wish that could be serialized). This method must return this object
   * @return not-null instance of Serializable you want to consider as your model's state
   */
  def startModelling: T
  def stopModelling()

  def onReceive() = {
    case m: EventMessage => handleMessage(m)
    case m: AntiMessage => handleMessage(m)
    case m: InfoMessage => logger warn m.text
    case m: TimeRequest => sendMessage(m.sender, TimeResponse(time, actorname))
    case StartMessage => setStateAndTime(0, startModelling)
    case StopMessage => stop()
    case _ => logger error "Unknown message"
  }

  /** model's time */
  private var time = 0d

  /** model's state */
  private var state: T = _

  /** @return model's time */
  def getTime = time

  /** @return model's state */
  def getState = state

  /**
   * Sets the time and the state<br><b>DON'T USE IT IN USER'S CODE!!!</b> Use {@link ru.tomtrix.synch.IModel#changeStateAndTime changeStateAndTime} instead
   * @param t time
   * @param s state
   */
  final def setStateAndTime(t: Double, s: T) {
    synchronized {
      time = t
      state = s
    }
  }

  final def stop() {
    synchronized {
      stopModelling()
      statFlush()
    }
  }

  /**
   * Changes the time and the state of a model
   * @param delta_t delta time
   * @param f function to modify the state
   * @example {{{  changeStateAndTime(5.5) {
   *   st => st.ball += 1
   * } }}}
   */
  def changeStateAndTime(delta_t: Double)(f: T => Unit) {
    synchronized {
      snapshot()
      time += delta_t
      f(state)
    }
  }

  /**
   * Sends message <b>m</b> to <b>whom</b>
   * @param whom receiver (actor name)
   * @param m message to send
   */
  def sendMessage(whom: String, m: Message) {
    backupMessage(whom, m)
    actors.get(whom) map {_ ! m} getOrElse logger.error(s"No such an actor: $whom")
  }

  /**
   * Sends InfoMessage to <b>whom</b>
   * @param whom receiver (actor name)
   * @param text message body
   */
  def sendMessage(whom: String, text: String) {
    sendMessage(whom, InfoMessage(actorname, text))
  }

  /**
   * Sends EventMessage to <b>whom</b>
   * @param whom receiver (actor name)
   * @param data message body
   */
  def sendMessage(whom: String, data: Serializable) {
    sendMessage(whom, EventMessage(time, actorname, data))
  }

  /**
   * Sends message <b>m</b> to all the actors listed in the conf-file
   * @param m message to send
   */
  def sendMessageToAll(m: Message) {
    actors foreach { t =>
      backupMessage(t._1, m)
      t._2 ! m
    }
  }

  /**
   * Sends InfoMessage to all the actors listed in the conf-file
   * @param text message body
   */
  def sendMessageToAll(text: String) {
    sendMessageToAll(InfoMessage(actorname, text))
  }

  /**
   * Sends EventMessage to all the actors listed in the conf-file
   * @param data message body
   */
  def sendMessageToAll(data: Serializable) {
    sendMessageToAll(EventMessage(time, actorname, data))
  }
}
