package ru.tomtrix.synch

import akka.actor._
import com.typesafe.config.ConfigFactory

/**
 * Trait that is responsible for sending and receiving messages
 */
trait Communicator[T <: Serializable] { self: Simulator[T] =>

  /**
   * Actor to receive messages
   */
  object Receiver extends Actor {
    def receive = onReceive()
  }

  /**
   * @return partial function that handles the received messages
   */
  def onReceive(): PartialFunction[Any, Unit]

  /** Configuration (from <b>application.conf</b>) */
  private val conf = ConfigFactory load()

  /** Akka system name */
  private val systemname = conf getString "actors.system"

  /** Addresses of neighbours */
  private val actorAddresses = conf.getStringList("actors.others").toArray(Array("")).toList

  /** Akka system */
  val system = ActorSystem(systemname)

  /** Actor's name*/
  val actorname = conf getString "actors.name"

  /** Neighbours' names */
  val actornames = actorAddresses map {_.split("/")(2)}

  /** Map actorname -> actor */
  val actors = (actornames zip actorAddresses.map{t => system actorFor s"akka://$systemname@$t"}).toMap

  system actorOf(Props(Receiver), name = actorname)
  logger info s"$actorname loaded"
}
