package ru.tomtrix.synch

import scala.util.Random
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Cancellable

/**
 * Created with IntelliJ IDEA.
 * User: Tom
 * Date: 08.04.13
 * Time: 19:28
 */
trait TestGenerator extends IModel[None.type ] {
  override def onReceive() = {
    case m: TimeResponse => checkTime()
    case _ => super.onReceive()
  }

  def checkTime() {

  }

  sendMessageToAll(StartMessage)
  system.scheduler.schedule(0 minutes, 500 milliseconds) {
    sendMessageToAll(TimeRequest(actorname))
  }
}
