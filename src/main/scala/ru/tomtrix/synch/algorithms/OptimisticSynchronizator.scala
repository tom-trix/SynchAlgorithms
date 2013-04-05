package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import ru.tomtrix.synch.Serializer._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.{AntiMessage, Message, EventMessage, IModel}
import collection.mutable.ListBuffer

/** Algorithm of classic optimistic synchronization */
trait OptimisticSynchronizator[T <: Serializable] { self: IModel[T] =>

  /** stack to keep the previous states */
  private val stateStack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()
  private val msgStack = new ConcurrentLinkedDeque[(Double, Message, String)]()
  private val inputQueue = new ListBuffer[Message]()

  /** Saves the state to a stack (to make it possible to rollback) */
  final def snapshot() {
    synchronized {
      stateStack push getTime -> serialize(getState)
      if (stateStack.size > 10000) {
        stateStack pollLast()
        logger error "stack overflown"
      }
    }
  }

  /** fse
   * @param m m */
  final def backupMessage(whom: String, m: Message) {
    synchronized {
      if (m.isInstanceOf[EventMessage]) {
        msgStack push (getTime, m, whom)
        if (msgStack.size > 10000) {
          msgStack pollLast()
          logger error "stack overflown"
        }
      }
    }
  }

  /** Performs rollback to a prevoius consistent state
   * @param t timestamp that corresponds to a prevoius consistent state */
  private def rollback(t: Double) {
    synchronized {
      log"Start rollback"
      var f = true
      var q: (Double, Array[Byte]) = null
      // pop all the states from the stack until find one with time ≤ t
      while (f) {
        q = stateStack poll()
        f = q!=null && q._1>t
      }
      // turn the time & the state back to found consistent state
      if (q != null)
        setStateAndTime(q._1, deserialize(q._2))
      // send anti-messages
      f = true
      var w: (Double, Message, String) = null
      while (f) {
        w = msgStack poll()
        if (w != null)
          sendMessage(w._3, AntiMessage(w._2))
        f = w!=null && w._1>t
      }
      log"Rolled back to t = $getTime"
    }
  }

  /** Handles received messages<br>
   * <b>ATTENTION! Don't forget to call <i>super</i> when overriding it!<b>
   * @param m message to handle */
  final def handleMessage(m: Message) {
    assert(m.isInstanceOf[EventMessage] || m.isInstanceOf[AntiMessage])
    if (m.t < getTime) rollback(m.t)
    // если такое же сообщение уже есть (т.е. мы получили антисообщение), то удаляем оба,
    // иначе просто добавляем сообщение во входную очередь
    else inputQueue find {_==m} map {t => inputQueue -= m} getOrElse {inputQueue += m}
  }

  /**
   * @return */
  def popMessage = {
    if (inputQueue.isEmpty) None
    else {
      val t = inputQueue(0)
      inputQueue -= t
      Some(t)
    }
  }
}
