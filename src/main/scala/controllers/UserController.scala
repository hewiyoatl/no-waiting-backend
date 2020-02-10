package controllers

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import auth.AuthService
import formatter.{Error, ErrorFormatter}
import javax.inject.Inject
import models.{UserIn, Users}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import services.{MailerService, MetricsService, RedirectService}
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
                               util: Util)
                              (implicit context: ExecutionContext) extends AbstractController(cc) {

  implicit val errorWriter = ErrorFormatter.errorWriter

  def profile = Action.async { request =>

    request.headers.get(HeaderNames.AUTHORIZATION) map { basicHeader =>

      val (user, passwordPlain) = authService.decodeBasicAuth(basicHeader)

      val password = authService.getSha256(passwordPlain)

      usersService.retrieveUser(user, password) map { userOpt =>
        //todo: check type of user admin or regular user. and return token

        userOpt map { user =>

          if (user.verifyEmail.getOrElse(false) == true) {
            val token = authService.provideTokenLogin(user)
            Ok(token).withHeaders(util.headersCors: _*)
          } else {
            BadRequest(Json.toJson(Error(BAD_REQUEST, "User has not verify his/her email")))
              .withHeaders(util.headersCors: _*)
          }
        } getOrElse (Forbidden(Json.toJson(Error(FORBIDDEN, "Unauthorized user or Invalid combinations user / password")))
          .withHeaders(util.headersCors: _*))
      }

    } getOrElse {

      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Missing authorization header")))
        .withHeaders(util.headersCors: _*))
    }

  }

  def resetAccount = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val email: String = (json \ "email").as[String].filter(_ != "")
      val languageOpt: String = (json \ "language").as[String].filter(_ != "")

      usersService.retrieveUser(email) map { userOpt =>

        userOpt map { user =>

          //if we got the user.

          //we need to update their retry and verify email to false

          val verifyEmailRetries = Some(user.verifyEmailRetry.getOrElse(0) + 1)

          usersService.updateVerifyEmail(user.email.getOrElse(""), Some(false), verifyEmailRetries)

          val newUser = UserIn(None, user.email.getOrElse(""), user.nickname, "", user.firstName.getOrElse(""), user.lastName.getOrElse(""), user.phoneNumber, "Client")

          sendEmailResetAccount(newUser, languageOpt)

          Ok("You will be receiving an email shortly to reset your account.").withHeaders(util.headersCors: _*)

        } getOrElse BadRequest(Json.toJson(Error(BAD_REQUEST, " User not found "))).withHeaders(util.headersCors: _*)
      }

    } getOrElse Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"Not received body correctly"))).withHeaders(util.headersCors: _*))

  }

  def resetAccountWithPassword = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val resetAccountToken: String = (json \ "resetAccountToken").as[String].filter(_ != "")
      val password: String = (json \ "password").as[String].filter(_ != "")
      val languageOpt: String = (json \ "language").as[String].filter(_ != "")

      authService.validateResetAccountJwt(resetAccountToken) match {

        case Success(claim) => {

          val email: String = (Json.parse(claim.content) \ "email").as[String]

          val result : Int = usersService.updateVerifyEmailAndPassword(authService.getSha256(password), email, Some(true), Some(0))

          result match {
            case 1 => Future(Ok("Your account has been updated correctly, please login").withHeaders(util.headersCors: _*))
            case _ => Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"Account was not updated correctly"))).withHeaders(util.headersCors: _*))
          }
        }

        case _ => {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"This token for reset account is not valid"))).withHeaders(util.headersCors: _*))
        }
      }

    } getOrElse Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"Not received body correctly"))).withHeaders(util.headersCors: _*))

  }

  def retryVerifyEmail = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson


    urlEncodedBody.map { json =>

      val email: String = (json \ "email").as[String].filter(_ != "")
      val languageOpt: String = (json \ "language").as[String].filter(_ != "")

      usersService.retrieveUser(email) map { userOpt =>

        userOpt map { user =>

          if (user.verifyEmail.getOrElse(false) == true) {
            BadRequest(Json.toJson(Error(BAD_REQUEST, "This user already verified his/her email"))).withHeaders(util.headersCors: _*)
          } else {

            val retryEmailCount = Some(user.verifyEmailRetry.getOrElse(1))

            usersService.updateVerifyEmail(email, Some(false), retryEmailCount)

            val newUser = UserIn(None, user.email.getOrElse(""), user.nickname, "", user.firstName.getOrElse(""), user.lastName.getOrElse(""), user.phoneNumber, "Client")

            sendEmailVerification(newUser, languageOpt)

            Ok("You will be receiving an email shortly").withHeaders(util.headersCors: _*)
          }

        } getOrElse BadRequest(Json.toJson(Error(BAD_REQUEST, " User not found "))).withHeaders(util.headersCors: _*)
      }

    } getOrElse Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"Not received body correctly"))).withHeaders(util.headersCors: _*))

  }

  def verifyEmail = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val verifyToken: String = (json \ "verifyEmailToken").as[String].filter(_ != "")

      authService.validateEmailJwt(verifyToken) match {

        case Success(claim) => {

          val emailToValidate: String = (Json.parse(claim.content) \ "email").as[String]
//          val passwordToValidate: String = (Json.parse(claim.content) \ "password").as[String]
          val languageToValidate: String = (Json.parse(claim.content) \ "language").as[String]

          usersService.retrieveUser(emailToValidate) map { userOpt =>

            userOpt map { user =>

              if (user.verifyEmail.getOrElse(false) == true) {
//                Redirect(redirectService.redirect(Some(languageToValidate), Some("app"), redirectService.KEY_FAILURE_PAGE))
//                  .withHeaders(("message", "Your email has already been verify"))
//                  .withHeaders(util.headers: _*)
                BadRequest(Json.toJson(Error(BAD_REQUEST, s"Your email has already been verify"))).withHeaders(util.headersCors: _*)
              } else {

                //finally verify and put the count retries to zero
                val resultUpdate: Int = usersService.updateVerifyEmail(user.email.getOrElse(""), Some(true), Some(0))
                resultUpdate match {
                  case 1 =>
//                    Redirect(redirectService.redirect(Some(languageToValidate), Some("app"), redirectService.KEY_SUCCESS_PAGE))
//                      .withHeaders(("message", "Update Email Verified correctly"))
//                      .withHeaders(util.headers: _*)
                    Ok("Update Email Verified correctly").withHeaders(util.headersCors: _*)
                  case _ =>

//                    Redirect(redirectService.redirect(Some(languageToValidate), Some("app"), redirectService.KEY_FAILURE_PAGE))
//                      .withHeaders(("message", "Your email was not able to update"))
//                      .withHeaders(util.headers: _*)

                  BadRequest(Json.toJson(Error(BAD_REQUEST, s"Your email was not able to update"))).withHeaders(util.headersCors: _*)
                }
              }
            } getOrElse Forbidden(Json.toJson(Error(FORBIDDEN, " User not found "))).withHeaders(util.headersCors: _*)
          }
        }

        case Failure(t) =>
//          Future(Redirect(redirectService.redirect(Some("en"), Some("app"), redirectService.KEY_FAILURE_PAGE))
//            .withHeaders(("message", "Not able to see this page"))
//            .withHeaders(util.headers: _*))
          Future(Forbidden(Json.toJson(Error(FORBIDDEN, "Token not longer valid "))).withHeaders(util.headersCors: _*))
      }
    } getOrElse Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "not well-formed"))).withHeaders(util.headersCors: _*))
  }

  def sendEmailVerification(newUser: UserIn, language: String): Unit = {

    val emailToken: String = authService.provideTokenEmailVerification(newUser, language)

    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

    mailerService.sendVerifyEmail(
      "Talachitas.com - Verify Email " + newUser.firstName + " " + newUser.lastName,
      newUser.email,
      newUser.firstName + " " + newUser.lastName + ", you have received successfully your verify email.",
      emailTokenEncoded
    )
  }

  def sendEmailResetAccount(newUser: UserIn, language: String): Unit = {

    val emailResetToken: String = authService.provideTokenResetAccount(newUser, language)

    val emailTokenEncoded: String = URLEncoder.encode(emailResetToken, StandardCharsets.UTF_8.toString)

    mailerService.sendResetAccount(
      "Talachitas.com - Reset Account " + newUser.firstName + " " + newUser.lastName,
      newUser.email,
      newUser.firstName + " " + newUser.lastName + ", you have received your reset account details.",
      emailTokenEncoded
    )
  }

  def addUser = Action.async { implicit request =>

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
      val languageOpt: Option[String] = (json \ "language").asOpt[String]

      emailOpt.map { email =>

        firstNameOpt.map { firstName =>

          lastNameOpt.map { lastName =>

            val newUser = UserIn(None, email, nicknameOpt, password, firstName, lastName, phoneOpt, "Client")

            usersService.verifyUser(email).flatMap { userExist =>

              userExist match {

                case true => {

                  Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"User ${email} already exists"))).withHeaders(util.headersCors: _*))
                }

                case false => {

                  usersService.addUser(newUser) map { userOutbound =>

                    sendEmailVerification(newUser, languageOpt.getOrElse("en"))

                    Ok(s"User ${userOutbound.get.email.getOrElse("")} created").withHeaders(util.headersCors: _*)
                  }

                }

              }

            }

          } getOrElse {

            Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "last name not defined"))).withHeaders(util.headersCors: _*))
          }

        } getOrElse {

          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "first name not defined"))).withHeaders(util.headersCors: _*))
        }

      } getOrElse {

        Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "email not defined"))).withHeaders(util.headersCors: _*))
      }

    } getOrElse {

      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "not well-formed"))).withHeaders(util.headersCors: _*))
    }
  }
}
