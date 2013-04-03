package ru.tomtrix.synch

import java.util.concurrent.ConcurrentLinkedDeque
import ru.tomtrix.Utils._
import ru.tomtrix.{EventMessage, IModel}

/**
 * r
 */
trait OptimisticSynchronizator { self: IModel =>

  private val stack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()

  def snapshot(t: Double) {
    stack push synchronized {
      t -> serialize(getState)
    }
    if (stack.size() > 10000) {
      stack.pollLast()
      logger error "stack overflown"
    }
  }

  private def rollback(t: Double) {
    var f = true
    var q: (Double, Array[Byte]) = null
    while(f) {
      q = stack.poll()
      f = q!=null && q._1>t
    }
    if (q != null) {
      setState(deserialize(q._2))
      addTime(q._1 - getTime)
      $("rollback")
    }
  }

  def handleMessage(m: EventMessage) {
    if (m.t < getTime)
      rollback(m.t)
    else snapshot(m.t)
  }
}
