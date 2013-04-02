package ru.tomtrix.synch

import java.util.concurrent.ConcurrentLinkedDeque
import ru.tomtrix.Utils._
import ru.tomtrix.{EventMessage, IModel}

/**
 * r
 */
trait OptimisticSynchronizator { self: IModel =>

  private val stack = new ConcurrentLinkedDeque[(Double, Array[Byte])]()

  private def shapshot(t: Double) {
    stack push synchronized {
      t -> serialize(state)
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
    if (q != null) synchronized {
      state = deserialize(q._2)
      time = q._1
    }
  }

  def handleMessage(m: EventMessage) {
    if (m.t < time)
      rollback(m.t)
    else shapshot(m.t)
  }
}
