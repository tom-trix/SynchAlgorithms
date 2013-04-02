package ru.tomtrix

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Agent that is responsible for sending and receiving the messages
 * @param model reference to a model
 */
class Communicator(model: IModel) extends Loggable {

  /** Actor to receive the messages (use <b>Props(new Receiver)</b>) */
  class Receiver extends Actor with Loggable {
    def receive = {
      case m: EventMessage => model handleMessage m
      case m: InfoMessage => logger warn m.text
      case StartMessage => model startModelling()
      case _ => logger error "Unknown message"
    }
  }

  private val conf = ConfigFactory load()

  val actorname = conf getString "actors.name"

  val actornames = conf.getStringList("actors.others").toArray(Array("")).toList

  private val systemname = conf getString "actors.system"

  private val system = ActorSystem(systemname)

  private val actor = system actorOf(Props(new Receiver), actorname)

  val actors = (actornames zip actornames.map{t => system actorFor s"akka://$systemname@$t"}).toMap

  logger info s"Actor $actor ($actorname) loaded"
}
