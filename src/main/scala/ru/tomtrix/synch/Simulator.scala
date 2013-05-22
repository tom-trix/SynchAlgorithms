package ru.tomtrix.synch

import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.structures._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.algorithms.OptimisticSynchronizator

/**
 * This abstract class is a 100% analog of [[ru.tomtrix.synch.Simulator Simulator]] and made for backward compatibility with Java
 * @tparam T any type that implements <b>Serializable</b>
 */
abstract class JavaSimulator[T <: Serializable] extends Simulator[T]

/**
 * Simulator is a core component of the whole modelling system
 * @tparam T any type that implements <b>Serializable</b>
 */
trait Simulator[T <: Serializable] extends Communicator[T] with OptimisticSynchronizator[T] with Loggable {

  /** model's time */
  private var time: Float = _

  /** model's state */
  private var state: T = _

  /**
   * The basic method you must implement. It'll be invoked as soon as the Starter sends a message to get started.
   * Your model ought to contain a state (any object you wish that implements <b>Serializable</b>). This method must return this state
   * @return not-null instance you want to consider as your model's state
   */
  def startModelling: T

  def onReceive() = {
    case m: BaseMessage => handleMessage(m)
    case m: LockRequest => handleLockRequest(m)
    case m: LockResponse => handleLockResponse()
    case m: TimeRequest => sendMessage(m.sender, TimeResponse(actorname, time))
    case m: StopMessage => sendMessage(m.sender, StatResponse(actorname, stopModelling()))
    case StartMessage => setStateAndTime(0, startModelling); snapshot(TimeEvent(0, null))
    case _ => logger error s"Unknown message"
  }

  override def sendMessage(whom: String, m: Message) {
    safe {
      super.sendMessage(whom, m)
      actors.get(whom) map {_ ! m} getOrElse logger.error(s"No such an actor: $whom")
      log"Послано сообщение $m"
    }
  }

  /**
   * Commits the event and changes the time of a model ( + performs a snapshot if it is necessary)
   * @param e handled time-event
   */
  override def commitEvent(e: TimeEvent) {
    safe {
      synchronized {
        if (e.t >= time)
          time = e.t
        else log"Обработано событие из прошлого $e"
        super.commitEvent(e)
      }
    }
  }

  /**
   * @return model's time
   */
  final def getTime = time

  /**
   * @return model's state
   */
  final def getState = state

  /**
   * Sets the time and the state<br>
   * <b>DON'T USE IT IN USER'S CODE!!!</b> Use [[ru.tomtrix.synch.Simulator#getState getState]] and [[ru.tomtrix.synch.Simulator#addTime addTime]] instead
   * @param t time
   * @param s state
   */
  final def setStateAndTime(t: Float, s: T) {
    synchronized {
      time = t
      state = s
    }
  }

  /**
   * Sends the message to all of the actors described in <b>actors.other</b> section of a <i>application.conf</i> file
   * @param m message to send
   */
  def sendMessageToAll(m: Message) {
    actornames foreach {sendMessage(_, m)}
  }
}
