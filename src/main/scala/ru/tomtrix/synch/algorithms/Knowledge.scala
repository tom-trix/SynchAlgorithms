package ru.tomtrix.synch.algorithms

import ru.tomtrix.synch.structures._

/**
 * Knowledge
 */
object Knowledge {
  def isIndependent(e: AgentEvent): Boolean = {
    Seq(
      e.patiens match {
        case "SuperMarket" => true
        case _ => false
      },
      e.predicate match {
        case "suspect" => true
        case _ => false
      }
    ) exists {t => t}
  }

  def cause(e: AgentEvent): Option[AgentEvent] = {
    e.predicate match {
      case "servePurchaser" => Some(AgentEvent(e.patiens, e.agens, "accepted"))
      case "requestToSmoke" => Some(AgentEvent(e.patiens, e.agens, "responseToSmoke"))
      case _ => None
    }
  }
}
