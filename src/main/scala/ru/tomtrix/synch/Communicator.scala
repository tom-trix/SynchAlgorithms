package ru.tomtrix.synch

import akka.actor._
import com.typesafe.config.ConfigFactory
import SafeCode._

/** Agent that is responsible for sending and receiving the messages */
trait Communicator[T <: Serializable] { self: IModel[T] =>

  /** Actor to receive the messages (use <b>Props(new Receiver)</b>) */
  class Receiver extends Actor {
    def receive = onReceive()
  }

  def onReceive(): PartialFunction[Any, Unit]

  private val conf = ConfigFactory load()

  val systemname = conf getString "actors.system"

  val system = ActorSystem(systemname)

  val actorname = conf getString "actors.name"

  val actor = system actorOf(Props(new Receiver), actorname)

  val actorsAddr = conf.getStringList("actors.others").toArray(Array("")).toList

  val actors = (actorsAddr zip actorsAddr.map{t => system actorFor s"akka://$systemname@$t"}).toMap

  val actornames = actorsAddr map {_.split("/")(2)}

  val workers = safe {
    conf.getStringList("workers.jars").toArray(Array("")).toList
  } getOrElse Nil

  logger info s"Actor $actor ($actorname) loaded"
}
