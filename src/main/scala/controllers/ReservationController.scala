package controllers

import formatter._
import javax.inject.Inject
import models.{Reservation, Reservations}
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.MetricsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class ReservationController @Inject()(cc: ControllerComponents, reservations: Reservations)
                                     (implicit context: ExecutionContext,
                                      metricsService: MetricsService) extends AbstractController(cc) {

  implicit val reservationReader = ReservationFormatter.ReservationReader

  implicit val reservationWriter = ReservationFormatter.ReservationWriter

  implicit val errorWriter = ErrorFormatter.errorWriter

  def listReservations(location: Option[Long]) = Action.async { implicit request =>
    reservations.listAll(location) map { reservations =>

      Ok(Json.toJson(reservations))
    }

  }

  def addReservation = Action.async { implicit request =>

    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[ReservationInbound] = json.validate[ReservationInbound]

        resultVal.asOpt.map { reservationInboud =>

          val reservationUser = Reservation(
            None,
            reservationInboud.userId,
            reservationInboud.userType,
            reservationInboud.sucursalId,
            Some(1),
            None)

          reservations.add(reservationUser) map { reservation =>

            Created(Json.toJson(reservation))
          }
        } getOrElse {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "new error"))))
        }
    } getOrElse {
      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Error"))))
    }
  }

  def deleteReservation(id: Long) = Action.async { implicit request =>
    reservations.delete(id)
    Future(NoContent)
  }

  def retrieveReservation(id: Long) = Action.async { implicit request =>
    reservations.retrieveReservation(id) map { reservation =>

      Ok(Json.toJson(reservation))
    }
  }

  def patchReservation(id: Long) = Action.async { implicit request =>

    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[ReservationInbound] = json.validate[ReservationInbound]

        resultVal.asOpt.map { reservationInboud =>

          val reservationReservation = Reservation(
            Some(id),
            reservationInboud.userId,
            reservationInboud.userType,
            reservationInboud.sucursalId,
            reservationInboud.status,
            None)

          reservations.patchReservation(reservationReservation) map { reservation =>

            Ok(Json.toJson(reservation))
          }
        } getOrElse {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "new error"))))
        }
    } getOrElse {
      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Error"))))
    }
  }

}
