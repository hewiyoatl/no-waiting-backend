package controllers

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import auth.AuthService
import formatter._
import javax.inject.Inject
import models._
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import services._
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class UserController @Inject()(cc: ControllerComponents,
                               usersService: Users,
                               addressService: Addresses,
                               authService: AuthService,
                               config: Configuration,
                               metricsService: MetricsService,
                               mailerService: MailerService,
                               redirectService: RedirectService,
                               interMessage: CustomizedLanguageService,
                               languageAction: LanguageAction,
                               util: Util)
                              (implicit context: ExecutionContext) extends AbstractController(cc) {

  implicit val userReader = UserFormatter.UserReader

  implicit val userWriter = UserFormatter.UserWriter

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

  def listUsers = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    usersService.listAll map { users =>
      val listUsers:Option[String] = Some(Json.stringify(Json.toJson(users)))
      val message: String = interMessage.customizedLanguageMessage(language, "user.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listUsers))).withHeaders(util.headersCors: _*)
    }
  }

  def login = languageAction.async { request =>

    val language: String = request.acceptLanguages.head.code

    request.headers.get(HeaderNames.AUTHORIZATION) map { basicHeader =>

      val (user, passwordPlain) = authService.decodeBasicAuth(basicHeader)

      val password = authService.getSha256(passwordPlain)

      usersService.retrieveUser(user, password) map { userOpt =>
        userOpt map { user =>
          if (user.verifyEmail.getOrElse(false) == true) {
            val token = authService.provideTokenLogin(user)
            val tokenString = Some(Json.stringify(Json.toJson(token)))
            val message: String = interMessage.customizedLanguageMessage(language, "user.login.success", "")
            Ok(Json.toJson(SuccessMessage(OK, message, tokenString))).withHeaders(util.headersCors: _*)
          } else {
            val message: String = interMessage.customizedLanguageMessage(language, "user.login.error.email", "")
            BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "user.login.error.invalid.password.account", "")
          Forbidden(Json.toJson(ErrorMessage(FORBIDDEN, message))).withHeaders(util.headersCors: _*)
        }
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.login.error.auth.header.missing", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def resetAccountSendNotification = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val email: String = (json \ "email").as[String].filter(_ != "")
//      val languageOpt: String = (json \ "language").as[String].filter(_ != "")

      usersService.retrieveUser(email) map { userOpt =>

        userOpt map { user =>

          //if we got the user.

          //we need to update their retry and verify email to false

          val verifyEmailRetries = Some(user.retryEmail.getOrElse(0) + 1)

          usersService.updateVerifyEmail(user.email, Some(false), verifyEmailRetries)

          val newUser = UserModelIn(None, user.email, user.nickname, Some(""), user.firstName, user.lastName, user.phoneNumber, "Client", None, None, None, None, None, None, None, Some(false))

          sendEmailResetAccount(newUser, language)

          val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.check.email", "")
          Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*)

        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.not.found", "")
          BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*)
        }
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def resetAccount = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val resetAccountToken: String = (json \ "resetAccountToken").as[String].filter(_ != "")
      val password: String = (json \ "password").as[String].filter(_ != "")
//      val languageOpt: String = (json \ "language").as[String].filter(_ != "")

      authService.validateResetAccountJwt(resetAccountToken) match {

        case Success(claim) => {

          val email: String = (Json.parse(claim.content) \ "email").as[String]
          val result : Int = usersService.updateVerifyEmailAndPassword(Some(authService.getSha256(password)), email, Some(true), Some(0))
          result match {
            case 1 => {
              val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.with.password.success")
              Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
            }
            case _ => {
              val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.with.password.error")
              Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
            }
          }
        }
        case _ => {
          val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.with.password.token.error")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.reset.account.with.password.body.error")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def retryVerifyEmailSendNotification = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val email: String = (json \ "email").as[String].filter(_ != "")
//      val languageOpt: String = (json \ "language").as[String].filter(_ != "")

      usersService.retrieveUser(email) map { userOpt =>

        userOpt map { user =>

          if (user.verifyEmail.getOrElse(false) == true) {
            val message: String = interMessage.customizedLanguageMessage(language, "user.retry.verify.email.already.verified", "")
            BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*)
          } else {

            val retryEmailCount = Some(user.retryEmail.getOrElse(1))

            usersService.updateVerifyEmail(email, Some(false), retryEmailCount)

            val newUser = UserModelIn(None, user.email, user.nickname, Some(""), user.firstName, user.lastName, user.phoneNumber, "Client", None, None, None, None, None, None, None, Some(false))

            sendEmailVerification(newUser, language)

            val message: String = interMessage.customizedLanguageMessage(language, "user.retry.verify.email.success", "")
            Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*)
          }

        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "user.retry.verify.email.user.not.found", "")
          BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*)
        }
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.retry.verify.email.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def verifyEmail = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val verifyToken: String = (json \ "verifyEmailToken").as[String].filter(_ != "")

      authService.validateEmailJwt(verifyToken) match {

        case Success(claim) => {

          val emailToValidate: String = (Json.parse(claim.content) \ "email").as[String]
//          val passwordToValidate: String = (Json.parse(claim.content) \ "password").as[String]
//          val languageToValidate: String = (Json.parse(claim.content) \ "language").as[String]

          usersService.retrieveUser(emailToValidate) map { userOpt =>

            userOpt map { user =>

              if (user.verifyEmail.getOrElse(false) == true) {
                val message: String = interMessage.customizedLanguageMessage(language, "user.verify.email.already.verified", "")
                BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*)
              } else {

                //finally verify and put the count retries to zero
                val resultUpdate: Int = usersService.updateVerifyEmail(user.email, Some(true), Some(0))
                resultUpdate match {
                  case 1 => {
                    val message: String = interMessage.customizedLanguageMessage(language, "user.verify.email.success", "")
                    Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*)
                  }
                  case _ => {
                    val message: String = interMessage.customizedLanguageMessage(language, "user.verify.email.error", "")
                    BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*)
                  }
                }
              }
            } getOrElse {
              val message: String = interMessage.customizedLanguageMessage(language, "user.verify.email.user.not.found", "")
              Forbidden(Json.toJson(ErrorMessage(FORBIDDEN, message))).withHeaders(util.headersCors: _*)
            }
          }
        }
        case Failure(t) => {
          val message: String = interMessage.customizedLanguageMessage(language, "user.verify.email.token.error", "")
          Future(Forbidden(Json.toJson(ErrorMessage(FORBIDDEN, message))).withHeaders(util.headersCors: _*))
        }
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.verify.email.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def addUser = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val emailOpt: Option[String] = (json \ "email").asOpt[String].filter(_ != "")
      val passwordOptPlan: Option[String] = (json \ "password").asOpt[String]
      val password: String = authService.getSha256(passwordOptPlan.getOrElse(""))
      val nicknameOpt: Option[String] = (json \ "nickname").asOpt[String]
      val firstNameOpt: Option[String] = (json \ "first_name").asOpt[String]
      val lastNameOpt: Option[String] = (json \ "last_name").asOpt[String]
      val phoneOpt: Option[String] = (json \ "phone").asOpt[String]
//      val languageOpt: Option[String] = (json \ "language").asOpt[String]

      emailOpt.map { email =>
        firstNameOpt.map { firstName =>
          lastNameOpt.map { lastName =>
            val newUser = UserModelIn(None, email, nicknameOpt, Some(password), firstName, lastName, phoneOpt, "Client", None, None, None, None, None, None, None, Some(false))
            usersService.verifyUser(email).flatMap { userExist =>
              userExist match {
                case true => {
                  val message: String = interMessage.customizedLanguageMessage(language, "user.add.already.exists", email)
                  Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
                }
                case false => {
                  usersService.addUser(newUser) map { userOutbound =>
                    sendEmailVerification(newUser, language)
                    val message: String = interMessage.customizedLanguageMessage(language, "user.add.success", email)
                    Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*)
                  }
                }
              }
            }
          } getOrElse {
            val message: String = interMessage.customizedLanguageMessage(language, "user.add.field.lastname")
            Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "user.add.field.firstname")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
      } getOrElse {
        val message: String = interMessage.customizedLanguageMessage(language, "user.add.field.email")
        Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
      }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.add.body.error")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def deleteUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    usersService.delete(id)
    val message: String = interMessage.customizedLanguageMessage(language, "user.delete.success")
    Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
  }

  def retrieveUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    usersService.retrieveUser(id) map { user =>
      val userString:Option[String] = Some(Json.stringify(Json.toJson(user)))
      val message: String = interMessage.customizedLanguageMessage(language, "user.retrieve.success")
      Ok(Json.toJson(SuccessMessage(OK, message, userString))).withHeaders(util.headersCors: _*)
    }
  }

  def patchUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[UserIn] = json.validate[UserIn]

        resultVal.asOpt.map { userInboud =>

          var addressId: Option[Long] = addressService.retrieveAddressPerPrimaryKey(userInboud.addressInfo).map(_.id).getOrElse(None)
          //if the address was not there in the table,
          //but user wants to update his/her address
          if (addressId.getOrElse(0) == 0 && addressService.receivingPrimaryKeyQuestion(userInboud.addressInfo)) {
            val addressResult: Option[Long] = addressService.add(userInboud.addressInfo.get)
            addressId = addressResult
          }

          val patchUser = UserModelIn(
            Some(id),
            userInboud.email,
            userInboud.nickname,
            Some(""),
            userInboud.firstName,
            userInboud.lastName,
            userInboud.phoneNumber,
            userInboud.roles,
            userInboud.verifyEmail,
            userInboud.verifyPhone,
            userInboud.retryEmail,
            userInboud.retryPhone,
            addressId,
            None,
            Some(DateTime.now()),
            Some(false))

          usersService.patchUser(patchUser) map { restaurant =>
            val restaurantString:Option[String] = Some(Json.stringify(Json.toJson(restaurant)))
            val message: String = interMessage.customizedLanguageMessage(language, "user.update.success")
            Ok(Json.toJson(SuccessMessage(OK, message, restaurantString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "user.update.error")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "user.body.error")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  private def sendEmailVerification(newUser: UserModelIn, language: String): Unit = {

    val subject: String = interMessage.customizedLanguageMessage(language, "user.send.email.verification.subject", newUser.firstName, newUser.lastName)
    val bodyMessage: String = interMessage.customizedLanguageMessage(language, "user.send.email.verification.body", newUser.firstName, newUser.lastName)
    val emailToken: String = authService.provideTokenEmailVerification(newUser, language)
    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

    mailerService.sendVerifyEmail(
      subject,
      newUser.email,
      bodyMessage,
      emailTokenEncoded
    )
  }

  private def sendEmailResetAccount(newUser: UserModelIn, language: String): Unit = {

    val subject: String = interMessage.customizedLanguageMessage(language, "user.send.email.reset.subject", newUser.firstName, newUser.lastName)
    val bodyMessage: String = interMessage.customizedLanguageMessage(language, "user.send.email.reset.body", newUser.firstName, newUser.lastName)

    val emailResetToken: String = authService.provideTokenResetAccount(newUser, language)
    val emailTokenEncoded: String = URLEncoder.encode(emailResetToken, StandardCharsets.UTF_8.toString)
    mailerService.sendResetAccount(
      subject,
      newUser.email,
      bodyMessage,
      emailTokenEncoded
    )
  }
}
