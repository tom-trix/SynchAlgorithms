package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import ru.tomtrix.synch.Serializer._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.{ EventMessage, IModel}

/** Algorithm of classic optimistic synchronization */
trait OptimisticSynchronizator[T <: Serializable] { self: IModel[T] =>

  /** stack to keep the previous states */
  private val stack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()

  /** Saves the state to a stack (to make it possible to rollback) */
  def snapshot() {
    synchronized {
      stack push getTime -> serialize(getState)
      if (stack.size() > 10000) {
        stack.pollLast()
        logger error "stack overflown"
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
      while (f) {
        q = stack poll()
        f = q != null && q._1 > t
      }
      if (q != null) {
        setStateAndTime(q._1, deserialize(q._2))
        log"Rolled back to t = $getTime"
      }
    }
  }

  /** Handles received messages<br>
   * <b>ATTENTION! Don't forget to call <i>super</i> when overriding it!<b>
   * @param m message to handle */
  def handleMessage(m: EventMessage) {
    if (m.t < getTime) rollback(m.t)
  }
}
