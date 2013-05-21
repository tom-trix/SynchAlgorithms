package ru.tomtrix.synch.models

import scala.util.Random
import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import akka.actor.Cancellable
import ru.tomtrix.synch._
import ru.tomtrix.synch.structures._

/**
 * Simple logic process for <b>TestGenerator</b> with a primitive incremental model
 */
object Worker extends App with Model[Stub] {
  /** random generator */
  val rand = new Random(Platform.currentTime)
  /** akka scheduler for periodically sending the messages*/
  var scheduler: Cancellable = _

  def suspendModelling(suspend: Boolean) {}
  def simulateStep(e: TimeEvent): Array[TimeEvent] = Array()
  def isLocal(e: AgentEvent) = true
  def startModelling = {
    scheduler = system.scheduler.schedule(0 seconds, 30 milliseconds) {
      synchronized {
        logger debug s"time = $getTime, state = ${getState.n}"
        getState.n += 1
        addTime(TimeEvent(rand.nextInt(10)+1, null))
        logger debug s"time = $getTime, state = ${getState.n}"
      }
      if (rand nextBoolean())
        sendMessageToAll(EventMessage(actorname, TimeEvent(getTime, AgentEvent("", "", ""))))
    }
    new Stub(0)
  }

  /*override*/ def onMessageReceived() {
    popMessage
    synchronized {
      logger debug s"time = $getTime, state = ${getState.n}"
      getState.n += 1
      addTime(TimeEvent(rand.nextInt(10)+1, null))
      logger debug s"time = $getTime, state = ${getState.n}"
    }
  }

  override def stopModelling() = {
    scheduler cancel()
    super.stopModelling()
  }
}
