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
        getState.n += 1
        addTime(rand.nextInt(10)+1)
        logger debug s"time = $getTime, state = ${getState.n}"
      }
      if (rand nextBoolean())
        sendMessageToAll(EVENT_MESSAGE(Stub(0)))
    }
    new Stub(0)
  }

  /*override*/ def onMessageReceived() {
    popMessage
    synchronized {
      logger debug s"time = $getTime, state = ${getState.n}"
      getState.n += 1
      addTime(rand.nextInt(10)+1)
      logger debug s"time = $getTime, state = ${getState.n}"
    }
  }

  override def stopModelling() = {
    scheduler cancel()
    super.stopModelling()
  }
}

/* HOW TO DO THE SAME IN JAVA
<dependency>
    <groupId>ru.tomtrix</groupId>
    <artifactId>asynch_algorithms</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.typesafe.akka</groupId>
    <artifactId>akka-actor_2.10</artifactId>
    <version>2.1.2</version>
</dependency>

import java.util.Random;
import java.util.concurrent.TimeUnit;
import scala.Function1;
import scala.collection.immutable.Map;
import scala.runtime.AbstractFunction1;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Cancellable;
import ru.tomtrix.synch.*;


public class Worker extends JavaModel<Stub>
{
    public static void main( String[] args ) {new Worker();}

    private final Random rand = new Random(System.currentTimeMillis());
    private final Worker worker = this;
    private Cancellable scheduler = null;

    @Override
    public Stub startModelling() {

        FiniteDuration fd = new FiniteDuration(30, TimeUnit.MILLISECONDS);
        scheduler = system().scheduler().schedule(fd, fd, new Runnable() {
            @Override
            public void run() {
                synchronized (worker) {
                    logger().debug(String.format("time = %.1f, state = %d", getTime(), getState().n()));
                    getState().javaInc();
                    addTime(1.0 + rand.nextInt(10));
                    logger().debug(String.format("time = %.1f, state = %d", getTime(), getState().n()));
                }
                if (rand.nextBoolean())
                    sendMessageToAll(new EventMessage(getTime(), actorname(), new scala.Some<Integer>(0)));
            }
        }, system().dispatcher());
        return new Stub(0);
    }

    @Override
    public void onMessageReceived() {
        popMessage();
        synchronized (worker) {
            logger().debug(String.format("time = %.1f, state = %d", getTime(), getState().n()));
            getState().javaInc();
            addTime(1.0 + rand.nextInt(10));
            logger().debug(String.format("time = %.1f, state = %d", getTime(), getState().n()));
        }
    }

    public Map<Category, Object> stopModelling() {
        scheduler.cancel();
        return super.stopModelling();
    }
}*/