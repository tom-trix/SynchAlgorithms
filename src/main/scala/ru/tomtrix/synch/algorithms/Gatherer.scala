package ru.tomtrix.synch.algorithms

import ru.tomtrix.synch.EventMessage
import ru.tomtrix.synch.ApacheLogger._

/**
 * Gatherer
 */
abstract class Done {
  val t: Double
  val hash: String
}
case class EventDone(t: Double, hash: String) extends Done
case class MessageDone(t: Double, hash: String, rolledBack: Boolean) extends Done
case class Bride(rolledBack: Boolean, context: Seq[String])

object Gatherer {
  var list: List[Done] = Nil

  def addEvent(ts: Double, hash: String) {
    val item = EventDone(ts, hash)
    list ::= item
  }

  def addMessage(currentTime: Double, receivedMessage: EventMessage) {
    val item = MessageDone(receivedMessage.t, receivedMessage.data.toHash, receivedMessage.t < currentTime)
    list ::= item
  }

  def onFinish() {
    var m = Map[String, List[Bride]]()
    val array = list.reverse.toArray
    for ((a, i) <- array.zipWithIndex)
      if (a.isInstanceOf[MessageDone]) {
        // смещаемся назад до актуальных записей (т.к. несколько записей м.б. неактуальны из-за роллбака)
        var j = i - 1
        while (j >= 0 && array(j).t > a.t)
          j -= 1
        // запомним кусок контекста
        var lst: List[String] = Nil
        var k = 0
        while (j-k >= 0 & array(j-k).isInstanceOf[EventDone]) {
          lst ::= array(j-k).hash
          k += 1
        }
        // alright!
        var brides = m.get(a.hash) getOrElse Nil
        brides ::= Bride(a.asInstanceOf[MessageDone].rolledBack, lst)
        m += a.hash -> brides.reverse
      }
    // print full list
    list.reverse foreach {p => log"$p"}
    // print processed list
    m foreach { t =>
      log"================ ${t._1} ================"
      t._2 sortBy{_.rolledBack} foreach {p => log"$p"}
    }
  }
}
