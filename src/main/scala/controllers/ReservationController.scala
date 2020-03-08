package controllers

import auth.AuthUserAction
import formatter._
import javax.inject.Inject
import models.{Reservation, _}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.http.HeaderNames
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services._
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
                                      authUserAction: AuthUserAction,
                                      mailerService: MailerService,
                                      reservationLogs: ReservationLogs,
                                      smsService: SmsService,
                                      config: Configuration,
                                      util: Util) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass())

  implicit val reservationReader = ReservationFormatter.ReservationReader

  implicit val reservationWriter = ReservationFormatter.ReservationWriter

  implicit val reservationWriterWithLogs = ReservationFormatter.ReservationWriterWithLogs

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
    reservations.listActiveReservation map { reservations =>
      val listReservations:Option[String] = Some(Json.stringify(Json.toJson(reservations)))
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listReservations))).withHeaders(util.headersCors: _*)
    }
  }

  def listArchiveReservations = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    reservations.listArchiveReservation map { reservations =>
      val listReservations:Option[String] = Some(Json.stringify(Json.toJson(reservations)))
      val message: String = interMessage.customizedLanguageMessage(language, "reservation.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listReservations))).withHeaders(util.headersCors: _*)
    }
  }


  def addReservation = languageAction.andThen(authUserAction) async { implicit request =>
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

            notificationsSmsEmailLogs(reservationInbound, userInfo, restaurantInfo, resId.getOrElse(0), language, request.headers.get(HeaderNames.AUTHORIZATION).getOrElse(""))

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

      val reservationLogsResult: Seq[ReservationLogsOutbound] = reservationLogs.listAllPerReservationSync(Some(id))
      val result: ReservationOutboundWithLogs = ReservationOutboundWithLogs(reservation.head, Some(reservationLogsResult))

      val reservationString:Option[String] = Some(Json.stringify(Json.toJson(result)))
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

            val userInfo: UserOutbound = usersService.retrieveUserSync(reservationInboud.userId).get
            val restaurantInfo: RestaurantOutbound = restaurants.retrieveRestaurantSync(reservationInboud.restaurantId).get

            notificationsSmsEmailLogs(reservationInboud, userInfo, restaurantInfo, id, language, request.headers.get(HeaderNames.AUTHORIZATION).getOrElse(""))

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

  private def notificationsSmsEmailLogs(reservationInboud: Reservation,
                                        userInfo: UserOutbound,
                                        restaurantInfo: RestaurantOutbound,
                                        reservationId: Long,
                                        language: String,
                                        authorization: String): Unit = {

    reservationInboud.status match {

      case ReservationStatus.STARTED => {

        //logs
        val reservationLog = ReservationLogsIn(None, Some(reservationId), Some(ReservationStatus.STARTED), reservationInboud.comments, None, None)
        reservationLogs.addSync(reservationLog)

        //notify via sms
        smsService.sendMessage(authorization, 2, userInfo.phoneNumber.getOrElse(""))

      }
        //to in queue, send email to customer
      case ReservationStatus.IN_QUEUE => {

        //logs
        val reservationLog = ReservationLogsIn(None, Some(reservationId), Some(ReservationStatus.IN_QUEUE), reservationInboud.comments, None, None)
        reservationLogs.addSync(reservationLog)

        //send email
        sendEmailNotificationCustomerInQueue(userInfo.firstName, userInfo.lastName, userInfo.email, language)

        //notify via sms
        smsService.sendMessage(authorization, 2, userInfo.phoneNumber.getOrElse(""))

      }

      case ReservationStatus.AVAILABLE => {

        //logs
        val reservationLog = ReservationLogsIn(None, Some(reservationId), Some(ReservationStatus.AVAILABLE), reservationInboud.comments, None, None)
        reservationLogs.addSync(reservationLog)


        val sourceAddress: AddressOutbound = userInfo.addressInfo.get
        val destinationAddress: AddressOutbound = restaurantInfo.addressInfo.get

        val sourceStr: String =
          sourceAddress.address1 + "+" +
            sourceAddress.address2 + "+" +
            sourceAddress.city + "+" +
            sourceAddress.state + "+" +
            sourceAddress.zipCode + "+" +
            sourceAddress.suffixZipCode + "+" +
            sourceAddress.country

        val destStr: String =
          destinationAddress.address1 + "+" +
            destinationAddress.address2 + "+" +
            destinationAddress.city + "+" +
            destinationAddress.state + "+" +
            destinationAddress.zipCode + "+" +
            destinationAddress.suffixZipCode + "+" +
            destinationAddress.country

        val travelMode: String = "DRIVING"
//        val googleMapsURL: String = s"""https://www.google.com/maps/dir/?api=1&origin=37.7033355,-122.424981&destination=884+Hermosa+Sunnyvale+CA&travelmode=DRIVING"""

//        val googleMapsURL: String = s"""https://www.google.com/maps/dir/?api=1&origin=$sourceStr&destination=$destStr&travelmode=$travelMode"""
        val googleMapsURL: String = s"""https://www.google.com/maps/dir/?api=1&destination=$destStr&travelmode=$travelMode"""

//        val googleMapsUrl: String = s"""https://maps.google.com?saddr=Current+Location&daddr=760+West+Genesee+Street+Syracuse+NY+13204"""

        //send email
        sendEmailNotificationCustomerAvailable(userInfo.firstName, userInfo.lastName, userInfo.email, googleMapsURL, language)

        //notify via sms
        smsService.sendMessage(authorization, 2, userInfo.phoneNumber.getOrElse(""))

      }

      case ReservationStatus.CANCELLED => {

        //logs
        val reservationLog = ReservationLogsIn(None, Some(reservationId), Some(ReservationStatus.CANCELLED), reservationInboud.comments, None, None)
        reservationLogs.addSync(reservationLog)

        //send email cancelled
        sendEmailNotificationCancelled(userInfo.firstName, userInfo.lastName, userInfo.email, language)

        //notify via sms
        smsService.sendMessage(authorization, 2, userInfo.phoneNumber.getOrElse(""))

      }

      case ReservationStatus.COMPLETED => {

        //logs
        val reservationLog = ReservationLogsIn(None, Some(reservationId), Some(ReservationStatus.COMPLETED), reservationInboud.comments, None, None)
        reservationLogs.addSync(reservationLog)


        //send email completed
        sendEmailNotificationCompleted(userInfo.firstName, userInfo.lastName, userInfo.email, language)


        //notify via sms
        smsService.sendMessage(authorization, 2, userInfo.phoneNumber.getOrElse(""))

      }
    }
  }
  private def sendEmailNotificationCustomerInQueue(firstName: String, lastName: String, email: String, language: String): Unit = {

    val subject: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.in_queue.subject", firstName, lastName)
    val welcomeMessageReplacement: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.in_queue.body", firstName, lastName)
//    val emailToken: String = authService.provideTokenEmailVerification(newUser, language)
//    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

    mailerService.sendEmailNotifyInQueue(
      subject,
      email,
      welcomeMessageReplacement)
  }

  private def sendEmailNotificationCustomerAvailable(firstName: String, lastName: String, email: String, googleMapsUrl: String, language: String): Unit = {

    val subject: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.available.subject", firstName, lastName)
    val welcomeMessageReplacement: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.available.body", firstName, lastName)
    //    val emailToken: String = authService.provideTokenEmailVerification(newUser, language)
    //    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

    mailerService.sendEmailNotifyAvailable(
      subject,
      email,
      welcomeMessageReplacement,
      googleMapsUrl)
  }

  private def sendEmailNotificationCancelled(firstName: String, lastName: String, email: String, language: String): Unit = {

    val subject: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.cancelled.subject", firstName, lastName)
    val welcomeMessageReplacement: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.cancelled.body", firstName, lastName)
    //    val emailToken: String = authService.provideTokenEmailVerification(newUser, language)
    //    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

    mailerService.sendEmailNotifyCancelled(
      subject,
      email,
      welcomeMessageReplacement)
  }

  private def sendEmailNotificationCompleted(firstName: String, lastName: String, email: String, language: String): Unit = {

    val subject: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.completed.subject", firstName, lastName)
    val welcomeMessageReplacement: String = interMessage.customizedLanguageMessage(language, "user.send.email.notify.completed.body", firstName, lastName)
    //    val emailToken: String = authService.provideTokenEmailVerification(newUser, language)
    //    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

    mailerService.sendEmailNotifyCompleted(
      subject,
      email,
      welcomeMessageReplacement)
  }

}
