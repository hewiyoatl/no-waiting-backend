package tasks

import akka.actor.ActorSystem
import javax.inject.Inject
import models.{ReservationOutbound, Reservations}
import org.joda.time.{DateTime, Seconds}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class CodeBlockTask @Inject() (actorSystem: ActorSystem, reservations: Reservations)
                              (implicit executionContext: ExecutionContext,
                               config: Configuration) {

  val logger: Logger = Logger(this.getClass())

  val intervalValueSeconds = 5

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  actorSystem.scheduler.schedule(initialDelay = 1.seconds, interval = intervalValueSeconds seconds) {

    // the block of code that will be executed
    logger.info("Checking status to update ...")

    val future = reservations.reservationsToProcess

    val responseRestaurant : Seq[ReservationOutbound] = Await.result(future, timeoutDatabaseSeconds)


    responseRestaurant.foreach(x => {

      val secondsBetweenCreationAndCurrent: Seconds = Seconds.secondsBetween(x.createdTimestamp.get, DateTime.now())

//      logger.info("Mauro 2 DateTime " + x.createdTimestamp.get + " " + DateTime.now() + " " + secondsBetweenCreationAndCurrent)


//      secondsBetweenCreationAndCurrent.

//      Minutes.minutesBetween(x.createdTimestamp.get, DateTime.now())

      reservations.updateWaitingTime(x.id.getOrElse(0), secondsBetweenCreationAndCurrent.getSeconds)
    })

    logger.info("Finishing status to update ...")

  }
}