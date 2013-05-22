package ru.tomtrix.synch.algorithms

import java.util.concurrent.ConcurrentLinkedDeque
import scala.collection.mutable.ListBuffer
import ru.tomtrix.synch._
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.Serializer._
import ru.tomtrix.synch.structures._
import ru.tomtrix.synch.StringUtils._
import ru.tomtrix.synch.ApacheLogger._

/**
 * Algorithm of an optimistic synchronization
 */
trait OptimisticSynchronizator[T <: Serializable] extends ModelObservable with Analyser[T] { self: Simulator[T] =>

  /** stack to keep the previous states */
  val stateStack = new ConcurrentLinkedDeque[(TimeEvent, Array[Byte])]()

  /** stack to keep the sent messages*/
  private val msgStack = new ConcurrentLinkedDeque[(EventMessage, String)]()

  /** special queue to store the input messages */
  private var inputQueue = ListBuffer[BaseMessage]()

  override def stopModelling() = {
    resetBuffers()
    super.stopModelling()
  }

  /**
   * Sends message <b>m</b> to <b>whom</b>
   * @param whom receiver (actor name)
   * @param m message to send
   */
  def sendMessage(whom: String, m: Message) {
    synchronized {
      m match {
        case em: EventMessage =>
          // если сообщение пришло "из прошлого", его нужно впихнуть в середину стека (аля Ханойские башни)
          var storage: List[(EventMessage, String)] = Nil
          while (msgStack.size > 0 && msgStack.peek._1.t > em.t)
            storage ::= msgStack pop()
          msgStack push (em, whom)
          for {a <- storage}
            msgStack push a
          statMessageSent(em)
        case am: AntiMessage => statMessageSent(am)
        case _ =>
      }
    }
  }

  /**
   * Commits the event and performs the snapshot
   * <br><b>BE CAREFUL!</b> this must be invoked <b>AFTER</b> the time is changed!
   * @param e handled time-event
   */
  override def commitEvent(e: TimeEvent) {
    snapshot(e)
    statEventHandled()
    super.commitEvent(e)
  }

  /**
   * Rather weird function...
   * <br>Returns true if <b>t</b> is less than message time: STRICTLY for Antimessages and NOT STRICTLY otherwise
   * @param t timestamp
   * @param m EventMessage or AntiMessage
   * @return
   */
  private def timeIsLessThanMessage(t: Double, m: BaseMessage): Boolean = m match {
    case _: EventMessage => t <= m.t
    case _: AntiMessage => t < m.t
    case _ => false
  }

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
            val am = AntiMessage(actorname, w._1)
            sendMessage(w._2, am)
            resumeByAntimessage(am)
            msgStack pop()
            msgStack peek()
          }

        //assert that everything is OK
        log"Time = ${getTime.roundBy(3)}; State = $getState"
        assert(getTime <= m.t, s"getTime = $getTime, t = ${m.t}")
        assert(msgStack.isEmpty || msgStack.peek()._1.t <= m.t)

        //full stack assertions
        var lst = stateStack.toArray(Array[(TimeEvent, Array[Byte])]()).toList map {_._1.t}
        if (lst.size > 0) lst reduce { (a, b) =>
          assert(a >= b, s"StateStack corrupted: $a < $b"); b
        }
        lst = msgStack.toArray(Array[(EventMessage, String)]()).toList map {_._1.t}
        if (lst.size > 0) lst reduce { (a, b) =>
          assert(a >= b, s"MessageStack corrupted: $a < $b"); b
        }
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
        // 1. preparing
        log"Принято сообщение $m"
        statMessageReceived(m)
        // 2. проверка на Rollback
        if (!timeIsLessThanMessage(getTime, m))
          m match {
            case em: EventMessage => if (!isOK(em.timeevent)) rollback(m)
            case _: AntiMessage => rollback(m)
          }
        // 3. если такое же сообщение уже есть (т.е. мы получили антисообщение), то удаляем оба, иначе просто добавляем сообщение во входную очередь
        m match {
          case _: AntiMessage => inputQueue find {_ == m} map {t =>
              inputQueue -= m
              log"Сообщения взаимно удалены: $m"
            } orElse {throw new RuntimeException(s"Unhandled $m")}
          case em: EventMessage =>
            inputQueue = (inputQueue += em).sorted
            checkUpMessage(em)
        }
      }
    }
  }

  /** Saves the state to a stack (to make it possible to rollback)<br>
    * <b>It unlikely might be used in user's code</b>
    */
  final def snapshot(e: TimeEvent) {
    synchronized {
      // если прищло безопасное событие "из прошлого" - его нужно впихнуть в середину стека (аля Ханойские башни)
      var storage: List[(TimeEvent, Array[Byte])] = Nil
      while (stateStack.size > 0 && stateStack.peek._1.t > e.t)
        storage ::= stateStack pop()
      stateStack push e -> serialize(getState)
      for {a <- storage}
        stateStack push a
      log"Time = ${getTime.roundBy(3)}; State = $getState"
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
