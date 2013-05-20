package ru.tomtrix.synch.algorithms

import scala.Some
import ru.tomtrix.synch._
import ru.tomtrix.synch.SafeCode._
import ru.tomtrix.synch.structures._

/**
 * GraphInfo
 */
case class GraphInfo(graph: Set[Node], prevNode: Node)

/**
 * GraphInfo
 */
trait AgentAnalyser[T <: Serializable] extends Loggable { self: Model[T] =>
  var graphs = Map[String, GraphInfo]()
  var timestamps = Map[String, Double]()
  var lockingEvent: Option[AgentEvent] = None

  def suspendModelling(suspend: Boolean)

  def simulateStep(e: AgentEvent): Array[AgentEvent]

  private def addNode(t: Double, agentname: String, node: Node) {
    synchronized {
      // add the node into a graph (since Graph is a set, multiply nodes will be resolved into a single one)
      // important! We connect nodes if and only if STRICTLY (previous.t < current.t)
      val nod = node.copy()
      graphs += agentname -> (graphs get agentname map {g: GraphInfo =>
        g.graph find {_ == nod} foreach {_.total += 1}
        g.graph find {_ == g.prevNode} foreach {p => if (timestamps.get(agentname).getOrElse(0d) < t) p.arcs connectNode nod}
        GraphInfo(g.graph + nod, nod)
      } getOrElse GraphInfo(Set(nod), nod))

      // remember the timestamp
      timestamps += agentname -> t
    }
  }

  private def addRollback(agentname: String, event: AgentEvent) {
    synchronized {
      for {
        graphinfo <- graphs get agentname
        node <- graphinfo.graph find {_.event == event}
      } yield node.rolledBack += 1
    }
  }

  private def forecastRollback(curNode: Node) {
    /*synchronized {
      for {
        graphinfo <- graphs get curNode.event.agent
        node <- graphinfo.graph find {_ == curNode}
      } yield {
        val neighs = node.arcs.getNodesAndProbabilities.keys
        if (curNode.communicationType == SENT)
          if (neighs.size == 1)
            if (neighs.head.communicationType == RECEIVED)
              if (neighs.head.rolledBack > 0)
                if (node.event.agent == neighs.head.event.recipient && node.event.recipient == neighs.head.event.agent) {
                  /*lockingEvent = Some(neighs.head.event)
                  suspendModelling(suspend = true)
                  sendMessage(convertToActor(lockingEvent.get), LockRequest(actorname))
                  logger debug s"Modelling is suspended! Detected: ${node.event}; waiting for ${lockingEvent.get}"*/
                  suspend(node.event, neighs.head.event)
                }
      }
    }*/
  }

  def onMessageReceived(m: EventMessage) {
    for {
      lock <- lockingEvent if m.timeevent.event == lock
    } resume()
  }

  def handleLockRequest(m: LockRequest) {
    logger debug "LockRequest received"
    lockingEvent foreach {t => sendMessage(m.sender, LockResponse(actorname))}
  }

  def handleLockResponse() {
    logger debug "LockResponse received :( Force resuming..."
    resume()
  }

  private def suspend(causedBy: AgentEvent, waitFor: AgentEvent, remoteActorname: String) {
    lockingEvent = Some(waitFor)
    logger debug s"Modelling is suspended! Detected: $causedBy; waiting for: $waitFor"
    suspendModelling(suspend = true)
    sendMessage(remoteActorname, LockRequest(actorname))
  }

  private def resume() {
    suspendModelling(suspend = false)
    lockingEvent = None
  }

  def registerEvent(e: TimeEvent, isSent: Boolean, isReceived: Boolean, remoteActorname: String) {
    assert(!(isSent && isReceived), "Message cannot be sent and received in the same time")
    val node = Node(e.event, if (isSent) SENT else if (isReceived) RECEIVED else LOCAL)
    addNode(e.t, e.event.agens, node)
    addNode(e.t, e.event.patiens, node)
    //forecastRollback(node)
    Knowledge cause e.event foreach { w =>
      if (isSent) suspend(e.event, w, remoteActorname)
    }
  }

  def registerRollback(event: AgentEvent) {
    resume()
    addRollback(event.agens, event)
    addRollback(event.patiens, event)
  }

  def messageIsSafe(m: BaseMessage): Boolean = {
    m match {
      case em: EventMessage => Knowledge isIndependent em.timeevent.event
      case _ => false
    }
  }

  def correlate(e1: AgentEvent, e2: AgentEvent): Boolean =
    e1.agens == e2.patiens || e1.patiens == e2.agens

  def rollbackIsSafe(e: TimeEvent): Boolean = {
    safe {
      synchronized {
        logger debug s"Stack contains ${stateStack.size()} elements"
        var result = true
        var storage: List[(TimeEvent, Array[Byte])] = Nil
        var q = stateStack peek()
        while (result && q != null)
          q = if (q._1.t < e.t) null
          else {
            if (correlate(e.event, q._1.event)) result = false
            storage ::= stateStack.pop()
            stateStack.peek()
          }
        for {a <- storage}
          stateStack push a
        logger debug s"Rollback is save = $result"
        logger debug s"Stack contains ${stateStack.size()} elements"
        result
      }
    } getOrElse false
  }

  def runPseudoEvent(m: EventMessage) {

  }

  def printGraphs() {
    graphs foreach { t =>
      logger debug (s"   === ${t._1} ===")
      t._2.graph foreach {logger debug _.toVerboseString}
    }
  }
}
