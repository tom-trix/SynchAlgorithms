package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import scala.Some
import scala.collection.mutable.ListBuffer
import ru.tomtrix.synch._
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.ApacheLogger._

/**
 * Algorithm of classic optimistic synchronization
 */
trait OptimisticSynchronizator[T <: {def cloneObject: T}] { self: IModel[T] =>

  /** stack to keep the previous states */
  private val stateStack = new ConcurrentLinkedDeque[(Double, T)]()

  /** stack to keep the sent messages*/
  private val msgStack = new ConcurrentLinkedDeque[(Message, String)]()

  /** special queue to store the input messages */
  private val inputQueue = new ListBuffer[Message]()

  /** map: actor -> GVT_estimate (so that GVT is a minimum of the estimates) */
  private var gvtMap = (actornames map {_ -> 0d}).toMap

  /**
   * Invoked as soon as a new EventMessage is received
   */
  def onMessageReceived()

  /**
   * Sends message <b>m</b> to <b>whom</b>
   * @param whom receiver (actor name)
   * @param m message to send
   */
  def sendMessage(whom: String, m: Message) {
    synchronized {
      if (m.isInstanceOf[EventMessage]) {
        msgStack push (m, whom)
        if (msgStack.size > 10000) {
          msgStack pollLast()
          logger error "stack overflown"
        }
      }
    }
  }

  /**
   * Calculates GVT and frees memory by removing all the states/messages with a timestamp that less than GVT
   * @return GVT
   */
  private def calculateGVTAndFreeMemory() = {
    val gvt = (gvtMap map { _._2 }).min
    log"gvt = $gvt"
    var q = stateStack peekLast()
    while (q != null)
      q = if (q._1 < gvt) {
        stateStack pollLast()
        stateStack peekLast()
      }
      else null
    var w = msgStack peekLast()
    while (w != null)
      w = if (w._1.t < gvt) {
        msgStack pollLast()
        msgStack peekLast()
      }
      else null
    gvt
  }

  /**
   * Performs rollback to a prevoius consistent state
   * @param m message that caused a rollback
   */
  private def rollback(m: Message) {
    safe {
      synchronized {
        log"ROLLBACK to ${m.t}"
        // pop all the states from the stack until find one with time ≤ t
        var depth = 1
        var q = stateStack poll()
        while (q != null)
          q = if (q._1 <= m.t) {
            statRolledback(depth, getTime-q._1)
            setStateAndTime(q._1, q._2)
            null
          }
          else {depth+=1; stateStack poll()}

        // send anti-messages
        var w = msgStack peek() //обязательно peek! Не факт, что элемент нужно будет удалить
        while (w != null)
          w = if (w._1.t > m.t) {
            sendMessage(w._2, new AntiMessage(w._1))
            msgStack pop()
            msgStack peek()
          }
          else null

        // finally calculate GVT & remove useless state/messages to free memory
        log"causedBy = ${m.sender}; map = $gvtMap"
        if (m.isInstanceOf[EventMessage])  //IMPORTANT!!!
          gvtMap += m.sender -> getTime
        val gvt = calculateGVTAndFreeMemory()

        //assert that everything is OK
        assert(getTime <= m.t, s"getTime = $getTime, t = ${m.t}")
        assert(msgStack.isEmpty || msgStack.peek()._1.t <= m.t)
        assert(msgStack.isEmpty || msgStack.peekLast()._1.t >= gvt)
        assert(stateStack.isEmpty || stateStack.peekLast()._1 >= gvt)
      }
    }
  }

  /**
   * Handles received messages
   * <b>It's unlikely used in user's code</b>
   * @param m message
   */
  final def handleMessage(m: Message) {
    safe {
      synchronized {
        assert(m.isInstanceOf[EventMessage] || m.isInstanceOf[AntiMessage])
        log"Принято сообщение $m"
        statMessageReceived(m)
        if (m.t < getTime) rollback(m)
        // если такое же сообщение уже есть (т.е. мы получили антисообщение), то удаляем оба, иначе просто добавляем сообщение во входную очередь
        else inputQueue find {_ == m} map {t => inputQueue -= m} getOrElse {inputQueue += m; onMessageReceived()}
        log"Statestack = $stateStack"
        log"Msgstack = $msgStack"
        log"InputQueue = $inputQueue"
      }
    }
  }

  /** Saves the state to a stack (to make it possible to rollback)<br>
    * <b>It's unlikely to be used in user's code</b> */
  final def snapshot() {
    synchronized {
      stateStack push getTime -> getState.cloneObject
      if (stateStack.size > 10000) {
        stateStack pollLast()
        logger error "stack overflown"
      }
    }
  }

  /**
   * Resets the algorithm by clearing all the buffers
   */
  final def resetBuffers() {
    synchronized {
      stateStack clear()
      msgStack clear()
      inputQueue clear()
    }
  }

  /**
   * Peeks whether the input buffer has a message. It doesn't remove the message
   * @return Option[message]
   */
  final def peekMessage = synchronized {
    if (inputQueue.isEmpty) None
    else Some(inputQueue(0))
  }

  /**
   * Extracts a message from the input buffer. It REMOVES the message if any
   * @return Option[message]
   */
  final def popMessage = synchronized {
    peekMessage map { t => inputQueue -= t; t }
  }
}
