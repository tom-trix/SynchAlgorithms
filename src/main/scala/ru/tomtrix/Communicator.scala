package ru.tomtrix

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Agent that is responsible for sending and receiving the messages
 */
trait Communicator[T <: Serializable] extends Loggable { self: IModel[T] =>

  /** Actor to receive the messages (use <b>Props(new Receiver)</b>) */
  class Receiver extends Actor {
    def receive = {
      case m: EventMessage => handleMessage(m)
      case m: InfoMessage => logger warn m.text
      case StartMessage => setState(startModelling)
      case _ => logger error "Unknown message"
    }
  }

  private val conf = ConfigFactory load()

  val systemname = conf getString "actors.system"

  val system = ActorSystem(systemname)

  val actorname = conf getString "actors.name"

  val actor = system actorOf(Props(new Receiver), actorname)

  val actornames = conf.getStringList("actors.others").toArray(Array("")).toList

  val actors = (actornames zip actornames.map{t => system actorFor s"akka://$systemname@$t"}).toMap

  logger info s"Actor $actor ($actorname) loaded"
}
