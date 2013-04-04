package ru.tomtrix.synch

import java.util.concurrent.ConcurrentLinkedDeque
import ru.tomtrix.Logg._
import ru.tomtrix.Utils._
import ru.tomtrix.{W, EventMessage, IModel}

/**
 * r
 */
trait OptimisticSynchronizator[T <: Serializable] { self: IModel[T] =>

  private val stack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()

  def snapshot() {
    synchronized {
      stack push getTime -> serialize(getState)
      if (stack.size() > 10000) {
        stack.pollLast()
        logger error "stack overflown"
      }
    }
  }

  private def rollback(t: Double) {
    synchronized {
      log"Start rollback"
      log"stack = $stack"
      var f = true
      var q: (Double, Array[Byte]) = null
      while (f) {
        q = stack poll()
        if (q!=null) log"q = $q (t=${q._1}, state = ${deserialize(q._2).asInstanceOf[W].i}})" else log"q=NULL"
        f = q != null && q._1 > t
      }
      if (q != null) {
        logger debug s"time = $getTime, state = ${getState.asInstanceOf[W].i}"
        log"rollback! (q = $q (t=${q._1}, state = ${deserialize(q._2).asInstanceOf[W].i}}))"
        setStateAndTime(q._1, deserialize(q._2).asInstanceOf[T])
        logger debug s"time = $getTime, state = ${getState.asInstanceOf[W].i}"
      }
    }
  }

  def handleMessage(m: EventMessage) {
    if (m.t < getTime) rollback(m.t)
  }
}
