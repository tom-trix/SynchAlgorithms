package ru.tomtrix

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

class Cello extends Actor {
  def receive = {
    case msg: String => println("joe received " + msg + " from " + sender)
    case _ => println("Received unknown msg ")
  }
}

object Server extends App {
  val conf = ConfigFactory.load()
  val systemname = conf.getString("actors.system")
  val system = ActorSystem(systemname)
  val actor = system.actorOf(Props[Cello], name = conf.getString("actors.name"))
  val actors = conf.getStringList("actors.others").toArray(Array("")).toList map {t => system.actorFor(s"akka://$systemname@$t")}
  println("Ready")

  while(true)
    actors foreach {_ ! readLine()}
}
