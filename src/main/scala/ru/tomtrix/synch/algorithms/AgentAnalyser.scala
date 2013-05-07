package ru.tomtrix.synch.algorithms

/**
 * GraphInfo
 */
case class GraphInfo(graph: Set[Node], prevNode: Node)

/**
 * GraphInfo
 */
trait AgentAnalyser {
  var graphs = Map[String, GraphInfo]()

  def newEvent(agent: String, recipient: String, action: String, isSent: Boolean, isReceived: Boolean) {
    assert(!(isSent && isReceived), "Message cannot be sent and received in the same time")

    // create a new node
    val node = Node(agent, recipient, action, if (isSent) SENT else if (isReceived) RECEIVED else LOCAL)

    // add the node into a graph (since Graph is a set, multiply nodes are not permitted)
    graphs += agent -> (graphs get agent map {t => GraphInfo(t.graph + node, {
      t.prevNode.arcs connectNode node
      node
    })} getOrElse GraphInfo(Set(node), node))
  }
}
