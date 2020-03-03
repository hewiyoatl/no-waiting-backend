package controllers

import formatter._
import javax.inject.Inject
import models._
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.{CustomizedLanguageService, LanguageAction, MetricsService}
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class ReservationController @Inject()(cc: ControllerComponents,
                                      reservations: Reservations,
                                      restaurants: Restaurants,
                                      usersService: Users)
                                     (implicit context: ExecutionContext,
                                      metricsService: MetricsService,
                                      interMessage: CustomizedLanguageService,
                                      languageAction: LanguageAction,
                                      config: Configuration,
                                      util: Util) extends AbstractController(cc) {

  implicit val reservationReader = ReservationFormatter.ReservationReader

  implicit val reservationWriter = ReservationFormatter.ReservationWriter

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

  def searchReservations(fields: String, values: String) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code

    if (fields == "status") {

    }

    reservations.listAll(values) map { reservations =>
      val listReservations:Option[String] = Some(Json.stringify(Json.toJson(reservations)))
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listReservations))).withHeaders(util.headersCors: _*)
    }
  }

  def listReservations = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    reservations.listAll map { reservations =>
      val listReservations:Option[String] = Some(Json.stringify(Json.toJson(reservations)))
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listReservations))).withHeaders(util.headersCors: _*)
    }
  }


  def addReservation = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json => {
        val resultVal: JsResult[Reservation] = json.validate[Reservation]

        resultVal.asOpt.map { reservationInbound =>

          val userInfo: UserOutbound = usersService.retrieveUserSync(reservationInbound.userId).get

          val restaurantInfo: RestaurantOutbound = restaurants.retrieveRestaurantSync(reservationInbound.restaurantId).get

          val currentTime = DateTime.now

          val reservationUser = ReservationModel(
            None,
            reservationInbound.userId,
            reservationInbound.restaurantId,
            ReservationStatus.STARTED,
            None,
            userInfo.addressInfo.get.id.get,
            restaurantInfo.addressInfo.get.id.get,
            restaurantInfo.averageWaitingTime,
            0L,
            Some(currentTime),
            Some(currentTime),
            false)


          reservations.add(reservationUser).map {  resId =>
            val message: String = interMessage.customizedLanguageMessage(language, "reservation.creation.success")
            Created(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*)
          }

        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "reservation.creation.error")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.body.error")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def deleteReservation(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    reservations.delete(id)
    val message: String = interMessage.customizedLanguageMessage(language, "reservation.delete.success")
    Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
  }

  def retrieveReservation(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    reservations.retrieveReservation(id) map { reservation =>
      val reservationString:Option[String] = Some(Json.stringify(Json.toJson(reservation)))
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.retrieve.success")
      Ok(Json.toJson(SuccessMessage(OK, message, reservationString))).withHeaders(util.headersCors: _*)
    }
  }

  def patchReservation(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[Reservation] = json.validate[Reservation]

        resultVal.asOpt.map { reservationInboud =>

          val currentTime = DateTime.now

          val reservationReservation = ReservationModel(
            Some(id),
            reservationInboud.userId,
            reservationInboud.restaurantId,
            reservationInboud.status,
            reservationInboud.comments,
            0L,//reservationInboud.sourceLatitude,
            0L,//reservationInboud.sourceLongitude,
            0L, // does not update
            0L, // does not update
            None,
            Some(currentTime),
            false)

          reservations.patchReservation(reservationReservation) map { reservation =>
            val reservationString:Option[String] = Some(Json.stringify(Json.toJson(reservation)))
            val message: String = interMessage.customizedLanguageMessage(language, "reservation.update.success", "")
            Ok(Json.toJson(SuccessMessage(OK, message, reservationString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "reservation.update.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
    }
  }
}
