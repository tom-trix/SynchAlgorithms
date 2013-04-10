package ru.tomtrix.synch

import scala.util.Random
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Cancellable
import ru.tomtrix.synch.MessageImplicits.EVENT_MESSAGE

case class W(var i: Long) {
  def cloneObject = copy()
}

object Worker extends App with IModel[W] {
  val rand = new Random(Platform.currentTime)
  var scheduler: Cancellable = _

  def startModelling = {
    scheduler = system.scheduler.schedule(0 seconds, 30 milliseconds) {
      synchronized {
        logger debug s"time = $getTime, state = ${getState.i}"
        changeStateAndTime(1 + rand.nextInt(10)){ t =>
          t.i += 1
        }
        logger debug s"time = $getTime, state = ${getState.i}"
      }
      if (rand nextBoolean())
        sendMessageToAll(EVENT_MESSAGE(Some(0)))
    }
    new W(0)
  }

  def onMessageReceived() {
    popMessage
    synchronized {
      logger debug s"time = $getTime, state = ${getState.i}"
      changeStateAndTime(1 + rand.nextInt(10)){ t =>
        t.i += 1
      }
      logger debug s"time = $getTime, state = ${getState.i}"
    }
  }

  override def stopModelling() = {
    scheduler cancel()
    super.stopModelling()
  }
}
