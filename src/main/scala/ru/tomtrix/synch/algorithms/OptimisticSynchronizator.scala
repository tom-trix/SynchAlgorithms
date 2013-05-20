package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import scala.collection.mutable.ListBuffer
import ru.tomtrix.synch._
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.Serializer._
import ru.tomtrix.synch.StringUtils._
import ru.tomtrix.synch.ApacheLogger._
import ru.tomtrix.synch.structures._

/**
 * Algorithm of classic optimistic synchronization
 */
trait OptimisticSynchronizator[T <: Serializable] extends AgentAnalyser[T] { self: Model[T] =>

  /** stack to keep the previous states */
  val stateStack = new ConcurrentLinkedDeque[(TimeEvent, Array[Byte])]()

  /** stack to keep the sent messages*/
  private val msgStack = new ConcurrentLinkedDeque[(EventMessage, String)]()

  /** special queue to store the input messages */
  private var inputQueue = ListBuffer[BaseMessage]()

  /** map: actor -> GVT_estimate (so that GVT is a minimum of the estimates) */
  private var gvtMap = (actornames map {_ -> 0d}).toMap

  /**
   * Sends message <b>m</b> to <b>whom</b>
   * @param whom receiver (actor name)
   * @param m message to send
   */
  def sendMessage(whom: String, m: Message) {
    synchronized {
      m match {
        case em: EventMessage =>
          msgStack push (em, whom)
          if (msgStack.size > 10000) {
            msgStack pollLast()
            logger error "stack overflown"
          }
        case _ =>
      }
    }
  }

  /**
   * Calculates GVT and frees memory by removing all the states/messages with a timestamp that less than GVT
   * @return GVT
   */
  private def calculateGVTAndFreeMemory() = {
    -1f //gvt вычисляется неверно!
    /*val gvt = (gvtMap map { _._2 }).min
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
    gvt*/
  }

  def timeIsLessThanMessage(t: Double, m: BaseMessage) =
    if (m.isInstanceOf[AntiMessage]) t < m.t else t <= m.t

  /**
   * Performs rollback to a prevoius consistent state
   * @param m message that caused a rollback
   */
  private def rollback(m: BaseMessage) {
    safe {
      synchronized {
        log"ROLLBACK to ${m.t roundBy 3}"
        // pop all the states from the stack until find one with time < t
        var depth = 1
        var q = stateStack peek() //обязательно peek!
        while (q != null)
          q = if (timeIsLessThanMessage(q._1.t, m)) {
            statRolledback(depth, getTime-q._1.t)
            setStateAndTime(q._1.t, deserialize(q._2))
            null
          }
          else {
            depth+=1
            stateStack pop()
            stateStack peek()
          }

        // send anti-messages
        var w = msgStack peek() //обязательно peek!
        while (w != null)
          w = if (timeIsLessThanMessage(w._1.t, m)) null
          else {
            sendMessage(w._2, new AntiMessage(actorname, w._1))
            msgStack pop()
            msgStack peek()
          }

        // calculate GVT & remove useless state/messages to free memory
        if (m.isInstanceOf[EventMessage])  //IMPORTANT!!!
          gvtMap += m.sender -> getTime
        val gvt = calculateGVTAndFreeMemory()

        // remember the rollback
        m match {
          case em: EventMessage => registerRollback(em.timeevent.event)
          case _ =>
        }

        //assert that everything is OK
        log"Time = ${getTime.roundBy(3)}; State = $getState"
        assert(getTime <= m.t, s"getTime = $getTime, t = ${m.t}")
        assert(msgStack.isEmpty || msgStack.peek()._1.t <= m.t)
        assert(msgStack.isEmpty || msgStack.peekLast()._1.t >= gvt)
        assert(stateStack.isEmpty || stateStack.peekLast()._1.t >= gvt)
      }
    }
  }

  /**
   * Handles received messages.<br>
   * <b>It unlikely might be used in user's code</b>
   * @param m message
   */
  final def handleMessage(m: BaseMessage) {
    safe {
      synchronized {
        assert(m.isInstanceOf[EventMessage] || m.isInstanceOf[AntiMessage])
        log"Принято сообщение $m"
        statMessageReceived(m)
        if (!timeIsLessThanMessage(getTime, m))
          if (messageIsSafe(m)) {
            log"Message $m is safe!!!"
            m.asInstanceOf[EventMessage].timeevent.event.isSafe = true
            //TODO перемешанность в стеке
          }
          else {
            if (m.isInstanceOf[EventMessage])
              rollbackIsSafe(m.asInstanceOf[EventMessage].timeevent)
            rollback(m)
          }
        // если такое же сообщение уже есть (т.е. мы получили антисообщение), то удаляем оба, иначе просто добавляем сообщение во входную очередь
        m match {
          case _: AntiMessage => inputQueue find {_ == m} map {t =>
              inputQueue -= m
              log"Сообщения взаимно удалены: $m"
            } orElse {throw new RuntimeException(s"Unhandled $m")}
          case em: EventMessage =>
            inputQueue = (inputQueue += em).sorted
            onMessageReceived(em)
        }
      }
    }
  }

  /** Saves the state to a stack (to make it possible to rollback)<br>
    * <b>It unlikely might be used in user's code</b>
    */
  final def snapshot(e: TimeEvent) {
    synchronized {
      stateStack push e -> serialize(getState)
      log"Time = ${getTime.roundBy(3)}; State = $getState"
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
    else Some(inputQueue.head.asInstanceOf[EventMessage])
  }

  /**
   * Extracts a message from the input buffer. It REMOVES the message if any
   * @return Option[message]
   */
  final def popMessage = synchronized {
    peekMessage map { t => inputQueue -= t; t }
  }
}
