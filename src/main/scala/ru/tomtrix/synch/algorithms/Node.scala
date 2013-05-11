package ru.tomtrix.synch.algorithms

sealed abstract class CommunicationType
case object LOCAL extends CommunicationType
case object SENT extends CommunicationType
case object RECEIVED extends CommunicationType

case class Node(agent: String, recipient: String, action: String, communicationType: CommunicationType) {
  val arcs = new ProbabilityArcSet
  var total = 1
  var rolledBack = 0

  def toVerboseString = {
    s"Node($agent, $recipient, $action, $communicationType; total=$total; rolledBack=$rolledBack; arcs=${arcs.getNodesAndProbabilities})"
  }
}

class ProbabilityArcSet {
  private var frequency = Map[Node, Int]()

  def getNodesAndProbabilities: Map[Node, Double] = {
    val sum: Double = (frequency map {_._2}).sum
    frequency map {t => t._1 -> t._2/sum}
  }

  def connectNode(node: Node) {
    frequency += node -> (frequency get node map {_ + 1} getOrElse 1)
  }
}


