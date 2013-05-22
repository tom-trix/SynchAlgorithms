package ru.tomtrix.synch.algorithms

import ru.tomtrix.synch.structures._

/**
 * Temporary object
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

  def isStateless(e: AgentEvent): Boolean = e.patiens match {
    case "Guard" => true
    case "SuperMarket" => true
    case s if s.startsWith("Purchaser") => true
    case _ => false
  }
}
