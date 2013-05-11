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

  def registerEvent(t: Double, event: AgentEvent, isSent: Boolean, isReceived: Boolean) {
    assert(!(isSent && isReceived), "Message cannot be sent and received in the same time")
    synchronized {
      // create a new node
      val node = Node(event, if (isSent) SENT else if (isReceived) RECEIVED else LOCAL)

      // add the node into a graph (since Graph is a set, multiply nodes will be resolved into a single one)
      // important! We connect nodes if and only if STRICTLY (previous.t < current.t)
      graphs += event.agent -> (graphs get event.agent map {g: GraphInfo =>
        g.graph find {_ == node} foreach {_.total += 1}
        g.graph find {_ == g.prevNode} foreach {p => if (timestamps.get(event.agent).getOrElse(0d) < t) p.arcs connectNode node}
        GraphInfo(g.graph + node, node)
      } getOrElse GraphInfo(Set(node), node))

      // remember the timestamp
      timestamps += event.agent -> t
    }
  }

  def registerRollback(event: AgentEvent) {
    for {
      graphinfo <- graphs get event.agent
      node <- graphinfo.graph find {_.event == event}
    } yield node.rolledBack += 1
  }

  def printGraphs() {
    graphs foreach { t =>
      logger debug (s"   === ${t._1} ===")
      t._2.graph foreach {logger debug _.toVerboseString}
    }
  }
}
