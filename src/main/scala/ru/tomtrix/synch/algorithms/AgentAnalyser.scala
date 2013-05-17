package ru.tomtrix.synch.algorithms

import scala.Some
import ru.tomtrix.synch._
import ru.tomtrix.synch.DeadlockMessage
import ru.tomtrix.synch.EventMessage

/**
 * GraphInfo
 */
case class GraphInfo(graph: Set[Node], prevNode: Node)

/**
 * GraphInfo
 */
trait AgentAnalyser[T <: HashSerializable] extends Loggable { self: Model[T] =>
  var graphs = Map[String, GraphInfo]()
  var timestamps = Map[String, Double]()
  var lockingEvent: Option[AgentEvent] = None

  def convertToEvent(m: EventMessage): AgentEvent

  def convertToActor(e: AgentEvent): String

  def suspendModelling()

  def resumeModelling()

  def handleDeadlockMessage()

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
    synchronized {
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
                  lockingEvent = Some(neighs.head.event)
                  suspendModelling()
                  sendMessage(convertToActor(lockingEvent.get), DeadlockMessage)
                  logger debug s"Modelling is suspended! Detected: ${node.event}; waiting for ${lockingEvent.get}"
                }
      }
    }
  }

  def onMessageReceived(m: EventMessage) {
    for {
      lock <- lockingEvent if convertToEvent(m) == lock
    } yield {
      lockingEvent = None
      resumeModelling()
    }
  }

  def registerEvent(t: Double, event: AgentEvent, isSent: Boolean, isReceived: Boolean) {
    assert(!(isSent && isReceived), "Message cannot be sent and received in the same time")
    val node = Node(event, if (isSent) SENT else if (isReceived) RECEIVED else LOCAL)
    addNode(t, event.agent, node)
    addNode(t, event.recipient, node)
    forecastRollback(node)
  }

  def registerRollback(event: AgentEvent) {
    addRollback(event.agent, event)
    addRollback(event.recipient, event)
  }

  def printGraphs() {
    graphs foreach { t =>
      logger debug (s"   === ${t._1} ===")
      t._2.graph foreach {logger debug _.toVerboseString}
    }
  }
}
