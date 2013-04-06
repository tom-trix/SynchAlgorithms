package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import scala.Some
import scala.collection.mutable.ListBuffer
import ru.tomtrix.synch._
import ru.tomtrix.synch.Serializer._
import ru.tomtrix.synch.ApacheLogger._

/** Algorithm of classic optimistic synchronization */
trait OptimisticSynchronizator[T <: Serializable] { self: IModel[T] =>

  def onMessageReceived()

  /** stack to keep the previous states */
  private val stateStack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()
  private val msgStack = new ConcurrentLinkedDeque[(Double, Message, String)]()
  private val inputQueue = new ListBuffer[Message]()

  /** Saves the state to a stack (to make it possible to rollback)<br>
   * <b>It's unlikely to be used in user's code</b> */
  final def snapshot() {
    synchronized {
      stateStack push getTime -> serialize(getState)
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

  /**
   * Performs rollback to a prevoius consistent state
   * @param t timestamp that corresponds to a prevoius consistent state
   */
  private def rollback(t: Double) {
    synchronized {
      log"ROLLBACK to $t"
      // pop all the states from the stack until find one with time ≤ t
      var q = stateStack poll()
      while (q != null)
        q = if (q._1 <= t) {
          setStateAndTime(q._1, deserialize(q._2))
          null
        }
        else stateStack poll()

      // send anti-messages
      var w = msgStack peek()
      while (w != null)
        w = if (w._1 > t) {
          sendMessage(w._3, AntiMessage(w._2))
          msgStack pop()
          msgStack peek() //обязательно peek! Не факт, что элемент нужно будет удалить
        }
        else null
    }
  }

  /**
   * Handles received messages
   * <b>It's unlikely to be used in user's code</b>
   * @param m message
   */
  final def handleMessage(m: Message) {
    synchronized {
      assert(m.isInstanceOf[EventMessage] || m.isInstanceOf[AntiMessage])
      log"Принято сообщение $m"
      if (m.t < getTime) rollback(m.t)
      // если такое же сообщение уже есть (т.е. мы получили антисообщение), то удаляем оба, иначе просто добавляем сообщение во входную очередь
      else inputQueue find {_ == m} map {t => inputQueue -= m} getOrElse {inputQueue += m; onMessageReceived()}
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
