package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import scala.Some
import scala.collection.mutable.ListBuffer
import ru.tomtrix.synch._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.SafeCode._

/** Algorithm of classic optimistic synchronization */
trait OptimisticSynchronizator[T <: Serializable] { self: IModel[T] =>

  def onMessageReceived()

  /** stack to keep the previous states */
  private val stateStack = new ConcurrentLinkedDeque[(Double, T)]()
  private val msgStack = new ConcurrentLinkedDeque[(Double, Message, String)]()
  private val inputQueue = new ListBuffer[Message]()
  private var gvtMap = (actornames map {_ -> 0d}).toMap

  /** Saves the state to a stack (to make it possible to rollback)<br>
   * <b>It's unlikely to be used in user's code</b> */
  final def snapshot() {
    synchronized {
      stateStack push getTime -> getState
      if (stateStack.size > 10000) {
        stateStack pollLast()
        logger error "stack overflown"
      }
    }
  }

  /**
   * Saves the message into a special buffer to make it possible to send antimessages afterwards<br>
   * <b>It's unlikely to be used in user's code</b>
   * @param whom receiver actor's name
   * @param m message
   */
  final def backupMessage(whom: String, m: Message) {
    synchronized {
      log"Послано сообщение $m"
      if (m.isInstanceOf[EventMessage]) {
        msgStack push (getTime, m, whom)
        if (msgStack.size > 10000) {
          msgStack pollLast()
          logger error "stack overflown"
        }
      }
    }
  }

  private def calculateGVTAndFreeMemory() = {
    val gvt = (gvtMap map { _._2 }).min
    var q = stateStack peekLast()
    while (q != null)
      q = if (q._1 < gvt) {
        stateStack pollLast()
        stateStack peekLast()
      }
      else null
    var w = msgStack peekLast()
    while (w != null)
      w = if (w._1 < gvt) {
        msgStack pollLast()
        msgStack peekLast()
      }
      else null
    gvt
  }

  /**
   * Performs rollback to a prevoius consistent state
   * @param causedBy actor that sent the message
   * @param t timestamp that corresponds to a prevoius consistent state
   */
  private def rollback(causedBy: String, t: Double) {
    synchronized {
      log"ROLLBACK to $t"
      // pop all the states from the stack until find one with time ≤ t
      var q = stateStack poll()
      while (q != null)
        q = if (q._1 <= t) {
          setStateAndTime(q._1, q._2)
          null
        }
        else stateStack poll()

      // send anti-messages
      var w = msgStack peek() //обязательно peek! Не факт, что элемент нужно будет удалить
      while (w != null)
        w = if (w._1 > t) {
          sendMessage(w._3, new AntiMessage(w._2))
          msgStack pop()
          msgStack peek()
        }
        else null

      // finally calculate GVT & remove useless state/messages to free memory
      gvtMap += causedBy -> t
      val gvt = calculateGVTAndFreeMemory()

      //assert that everything is OK
      assert(getTime <= t)
      assert(msgStack.isEmpty || msgStack.peek()._1 <= t)
      assert(msgStack.isEmpty || msgStack.peekLast()._1 >= gvt)
      assert(stateStack.isEmpty || stateStack.peekLast()._1 >= gvt)
    }
  }

  /**
   * Handles received messages
   * <b>It's unlikely to be used in user's code</b>
   * @param m message
   */
  final def handleMessage(m: Message) {
    safe {
      synchronized {
        assert(m.isInstanceOf[EventMessage] || m.isInstanceOf[AntiMessage])
        log"Принято сообщение $m"
        if (m.t < getTime) rollback(m.sender, m.t)
        // если такое же сообщение уже есть (т.е. мы получили антисообщение), то удаляем оба, иначе просто добавляем сообщение во входную очередь
        else inputQueue find {_ == m} map {t => inputQueue -= m} getOrElse {inputQueue += m; onMessageReceived()}
      }
    }
  }

  /**
   * Peeks whether the input buffer has a message. It doesn't remove the message anyway
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
