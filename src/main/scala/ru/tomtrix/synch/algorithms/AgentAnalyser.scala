package ru.tomtrix.synch.algorithms

import ru.tomtrix.synch.{EventMessage, Loggable}

/**
 * GraphInfo
 */
case class GraphInfo(graph: Set[Node], prevNode: Node)

/**
 * GraphInfo
 */
trait AgentAnalyser extends Loggable {
  var graphs = Map[String, GraphInfo]()
  var timestamps = Map[String, Double]()

  def convertRollback(m: EventMessage): AgentEvent

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

  def registerEvent(t: Double, event: AgentEvent, isSent: Boolean, isReceived: Boolean) {
    assert(!(isSent && isReceived), "Message cannot be sent and received in the same time")
    val node = Node(event, if (isSent) SENT else if (isReceived) RECEIVED else LOCAL)
    addNode(t, event.agent, node)
    addNode(t, event.recipient, node)
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
