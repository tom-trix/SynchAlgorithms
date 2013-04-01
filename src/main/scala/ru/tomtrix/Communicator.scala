package ru.tomtrix

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Agent that is responsible for sending and receiving the messages
 * @param model reference to a model
 */
class Communicator(model: ModelLoadable) extends Loggable {

  /** Actor to receive the messages (use <b>Props(new Receiver)</b>) */
  class Receiver extends Actor with Loggable {
    def receive = {
      case StartMessage => model startModelling()
      case msg: String => logger info msg
      case _ => logger error "Unknown message"
    }
  }

  val conf = ConfigFactory load()
  val systemname = conf getString "actors.system"
  val actorname = conf getString "actors.name"
  val system = ActorSystem(systemname)
  val actor = system actorOf(Props(new Receiver), actorname)
  val actors = conf.getStringList("actors.others").toArray(Array("")).toList map {t => system actorFor s"akka://$systemname@$t"}

  logger info s"Actor $actorname loaded"
}
