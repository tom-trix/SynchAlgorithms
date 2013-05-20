package ru.tomtrix.synch

import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.algorithms.OptimisticSynchronizator
import ru.tomtrix.synch.structures._

/**
 * Abstract trait that your model should implement
 * @tparam T any type that implements <b>Serializable</b>
 */
trait Model[T <: Serializable] extends Communicator[T] with ModelObservable with OptimisticSynchronizator[T] with Loggable {

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

  override def stopModelling() = {
    printGraphs()
    resetBuffers()
    super.stopModelling()
  }

  def onReceive() = {
    case m: BaseMessage => handleMessage(m)
    case m: LockRequest => handleLockRequest(m)
    case m: LockResponse => handleLockResponse()
    case m: TimeRequest => sendMessageToStarter(TimeResponse(actorname, time))
    case StartMessage => setStateAndTime(0, startModelling); snapshot(null)
    case StopMessage => sendMessageToStarter(StatResponse(actorname, stopModelling()))
    case _ => logger error s"Unknown message"
  }

  override def sendMessage(whom: String, m: Message) {
    safe {
      super.sendMessage(whom, m)
      m match {
        case bm: BaseMessage => statMessageSent(bm)
        case _ =>
      }
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
   * <b>DON'T USE IT IN USER'S CODE!!!</b> Use {@link ru.tomtrix.synch.Model#getState getState} and {@link ru.tomtrix.synch.Model#addTime addTime} instead
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
   * Changes the time of a model and performs a snapshot if it is necessary
   */
  final def addTime(e: TimeEvent) {
    safe {
      synchronized {
        if (e.t < time) throw new RuntimeException("Time is wrapped!")
        time = e.t
        snapshot(e)
      }
    }
  }

  /**
   * Sends the message to a Starter actor described in <b>actors.starter</b> section of a <i>application.conf</i> file
   * @param m message to send
   */
  def sendMessageToStarter(m: Message) {
    safe {
      starter foreach { _ ! m}
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

/**
 * This abstract class is a 100% analog of Model and made for backward compatibility with Java
 * @tparam T any type that implements <b>Serializable</b>
 */
abstract class JavaModel[T <: Serializable] extends Model[T]