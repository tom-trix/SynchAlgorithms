package ru.tomtrix.synch

import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.MessageImplicits._
import ru.tomtrix.synch.algorithms.OptimisticSynchronizator

/**
 * Abstract trait that your model should implement
 */
trait IModel[T <: {def cloneObject: T}] extends Communicator[T] with ModelObservable with OptimisticSynchronizator[T] with Loggable {

  /** model's time */
  private var time: Double = _

  /** model's state */
  private var state: T = _

  /**
   * The basic method you must implement. It'll be invoked as soon as the Starter sends a message to get started.
   * Your model ought to contain a state (any object you wish that could be serialized). This method must return this object
   * @return not-null instance of Serializable you want to consider as your model's state
   */
  def startModelling: T

  override def stopModelling() = {
    resetBuffers()
    super.stopModelling()
  }

  def onReceive() = {
    case m: EventMessage => handleMessage(m)
    case m: AntiMessage => handleMessage(m)
    case m: InfoMessage => logger info m.text
    case TimeRequest => sendMessageToStarter(TIME_RESPONSE)
    case StopMessage => sendMessageToStarter(STAT_RESPONSE(stopModelling()))
    case StartMessage => setStateAndTime(0, startModelling)
    case _ => logger error s"Unknown message (state = ${state.cloneObject})"
  }

  override def sendMessage(whom: String, m: Message) {
    safe {
      super.sendMessage(whom, m)
      if (m.isInstanceOf[EventMessage] || m.isInstanceOf[AntiMessage])
        statMessageSent(m)
      actors.get(whom) map {_ ! m} getOrElse logger.error(s"No such an actor: $whom")
      log"Послано сообщение $m"
    }
  }

  /** @return model's time */
  final def getTime = time

  /** @return model's state */
  final def getState = state

  /**
   * Sets the time and the state<br>
   * <b>DON'T USE IT IN USER'S CODE!!!</b> Use {@link ru.tomtrix.synch.IModel#changeStateAndTime changeStateAndTime} instead
   * @param t time
   * @param s state
   */
  final def setStateAndTime(t: Double, s: T) {
    synchronized {
      time = t
      state = s
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
  final def changeStateAndTime(delta_t: Double)(f: T => Unit) {
    safe {
      synchronized {
        snapshot()
        time += delta_t
        f(state)
      }
    }
  }

  /**
   * Sends the message to a Starter actor described in <b>cators.starter</b> section of a config file
   * @param m message to send
   */
  def sendMessageToStarter(m: Message) {
    safe {
      starter foreach { _ ! m}
    }
  }

  /**
   * Sends the message to all of the actors described in <b>cators.others</b> section of a config file
   * @param m message to send
   */
  def sendMessageToAll(m: Message) {
    actornames foreach {sendMessage(_, m)}
  }

  implicit def toMessage(x: TIME_RESPONSE.type): TimeResponse = TimeResponse(getTime, actorname)
  implicit def toMessage(x: INFO_MESSAGE): InfoMessage = InfoMessage(actorname, x.text)
  implicit def toMessage(x: STAT_RESPONSE): StatResponse = StatResponse(getTime, actorname, x.stat)
  implicit def toMessage(x: EVENT_MESSAGE): EventMessage = EventMessage(getTime, actorname, x.data)
}
