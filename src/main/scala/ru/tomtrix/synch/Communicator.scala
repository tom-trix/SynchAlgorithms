package ru.tomtrix.synch

import akka.actor._
import com.typesafe.config.ConfigFactory
import ru.tomtrix.synch.SafeCode._

/**
 * Agent that is responsible for sending and receiving messages
 */
trait Communicator[T <: Serializable] { self: Model[T] =>

  /** Actor to receive messages */
  object Receiver extends Actor {
    def receive = onReceive()
  }

  /**
   * @return Partial function that handles the received messages
   */
  def onReceive(): PartialFunction[Any, Unit]

  private val conf = ConfigFactory load()

  val systemname = conf getString "actors.system"

  val system = ActorSystem(systemname)

  val actorname = conf getString "actors.name"

  val actor = system actorOf(Props(Receiver), actorname)

  val actorAddresses = conf.getStringList("actors.others").toArray(Array("")).toList

  val actornames = actorAddresses map {_.split("/")(2)}

  val actors = (actornames zip actorAddresses.map{t => system actorFor s"akka://$systemname@$t"}).toMap

  val starter = safe$ {
    system actorFor s"akka://$systemname@${conf getString "actors.starter"}"
  }

  logger info s"Actor $actor ($actorname) loaded"
}
