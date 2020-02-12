package controllers

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import auth.AuthService
import formatter._
import javax.inject.Inject
import models.{UserIn, Users}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import services._
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class UserController @Inject()(cc: ControllerComponents,
                               usersService: Users,
                               authService: AuthService,
                               config: Configuration,
                               metricsService: MetricsService,
                               mailerService: MailerService,
                               redirectService: RedirectService,
                               interMessage: CustomizedLanguageService,
                               languageAction: LanguageAction,
                               util: Util)
                              (implicit context: ExecutionContext) extends AbstractController(cc) {

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

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

          val verifyEmailRetries = Some(user.verifyEmailRetry.getOrElse(0) + 1)

          usersService.updateVerifyEmail(user.email.getOrElse(""), Some(false), verifyEmailRetries)

          val newUser = UserIn(None, user.email.getOrElse(""), user.nickname, "", user.firstName.getOrElse(""), user.lastName.getOrElse(""), user.phoneNumber, "Client")

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
          val result : Int = usersService.updateVerifyEmailAndPassword(authService.getSha256(password), email, Some(true), Some(0))
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

            val retryEmailCount = Some(user.verifyEmailRetry.getOrElse(1))

            usersService.updateVerifyEmail(email, Some(false), retryEmailCount)

            val newUser = UserIn(None, user.email.getOrElse(""), user.nickname, "", user.firstName.getOrElse(""), user.lastName.getOrElse(""), user.phoneNumber, "Client")

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
                val resultUpdate: Int = usersService.updateVerifyEmail(user.email.getOrElse(""), Some(true), Some(0))
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
            val newUser = UserIn(None, email, nicknameOpt, password, firstName, lastName, phoneOpt, "Client")
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

  private def sendEmailVerification(newUser: UserIn, language: String): Unit = {

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

  private def sendEmailResetAccount(newUser: UserIn, language: String): Unit = {

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
