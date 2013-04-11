package ru.tomtrix.synch

import scala.util.Random
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Cancellable
import ru.tomtrix.synch.MessageImplicits.EVENT_MESSAGE

/**
 * Simple logic process for <b>TestGenerator</b> with a primitive incremental model
 */
object Worker extends App with Model[Stub] {
  /** random generator */
  val rand = new Random(Platform.currentTime)
  /** akka scheduler for periodically sending the messages*/
  var scheduler: Cancellable = _

  def startModelling = {
    scheduler = system.scheduler.schedule(0 seconds, 30 milliseconds) {
      synchronized {
        logger debug s"time = $getTime, state = ${getState.n}"
        changeStateAndTime(1 + rand.nextInt(10)){ t =>
          t.n += 1
        }
        logger debug s"time = $getTime, state = ${getState.n}"
      }
      if (rand nextBoolean())
        sendMessageToAll(EVENT_MESSAGE(Some(0)))
    }
    new Stub(0)
  }

  def onMessageReceived() {
    popMessage
    synchronized {
      logger debug s"time = $getTime, state = ${getState.n}"
      changeStateAndTime(1 + rand.nextInt(10)){ t =>
        t.n += 1
      }
      logger debug s"time = $getTime, state = ${getState.n}"
    }
  }

  override def stopModelling() = {
    scheduler cancel()
    super.stopModelling()
  }
}
