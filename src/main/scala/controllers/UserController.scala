package controllers

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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


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

          val token = authService.provideToken(user)

          Ok(token).withHeaders(util.headers: _*)

        } getOrElse (Forbidden(Json.toJson(Error(FORBIDDEN, "Unauthorized user")))
          .withHeaders(util.headers: _*))

      }

    } getOrElse {

      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Missing authorization header")))
        .withHeaders(util.headers: _*))
    }

  }

  def verifyEmail = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val verifyToken: String = (json \ "verifyEmailToken").as[String].filter(_ != "")

      authService.validateEmailJwt(verifyToken) match {

        case Success(claim) => {

          val emailToValidate: String = (Json.parse(claim.content) \ "email").as[String]
          val passwordToValidate: String = (Json.parse(claim.content) \ "password").as[String]
          val languageToValidate: String = (Json.parse(claim.content) \ "language").as[String]

          usersService.retrieveUser(emailToValidate, passwordToValidate) map { userOpt =>

            userOpt map { user =>

              if (user.verifyEmail.getOrElse(false) == true) {
//                Redirect(redirectService.redirect(Some(languageToValidate), Some("app"), redirectService.KEY_FAILURE_PAGE))
//                  .withHeaders(("message", "Your email has already been verify"))
//                  .withHeaders(util.headers: _*)
                BadRequest(Json.toJson(Error(BAD_REQUEST, s"Your email has already been verify"))).withHeaders(util.headers: _*)
              } else {

                val resultUpdate: Int = usersService.updateVerifyEmail(user.email.getOrElse(""), user.verifyEmailRetry.getOrElse(0))
                resultUpdate match {
                  case 1 =>
//                    Redirect(redirectService.redirect(Some(languageToValidate), Some("app"), redirectService.KEY_SUCCESS_PAGE))
//                      .withHeaders(("message", "Update Email Verified correctly"))
//                      .withHeaders(util.headers: _*)
                    Ok("Update Email Verified correctly").withHeaders(util.headers: _*)
                  case _ =>

//                    Redirect(redirectService.redirect(Some(languageToValidate), Some("app"), redirectService.KEY_FAILURE_PAGE))
//                      .withHeaders(("message", "Your email was not able to update"))
//                      .withHeaders(util.headers: _*)

                  BadRequest(Json.toJson(Error(BAD_REQUEST, s"Your email was not able to update"))).withHeaders(util.headers: _*)
                }
              }
            } getOrElse Forbidden(Json.toJson(Error(FORBIDDEN, " User not found "))).withHeaders(util.headers: _*)
          }
        }

        case Failure(t) =>
//          Future(Redirect(redirectService.redirect(Some("en"), Some("app"), redirectService.KEY_FAILURE_PAGE))
//            .withHeaders(("message", "Not able to see this page"))
//            .withHeaders(util.headers: _*))
          Future(Forbidden(Json.toJson(Error(FORBIDDEN, "Token not longer valid "))).withHeaders(util.headers: _*))
      }
    } getOrElse Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "not well-formed"))).withHeaders(util.headers: _*))
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

                  Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"User ${email} already exists"))).withHeaders(util.headers: _*))
                }

                case false => {

                  usersService.addUser(newUser) map { userOutbound =>

                    val emailToken: String = authService.provideTokenEmailVerification(newUser, languageOpt.getOrElse("en"))

                    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

                    mailerService.sendVerifyEmail(
                      "Talachitas.com - Verify Email " + firstName + " " + lastName,
                      "talachitasus@gmail.com",
                      email,
                      "mauricio.gomez.77@gmail.com",
                      firstName + " " + lastName + ", you have received successfully your verify email.",
                      emailTokenEncoded
                    )

                    Ok(s"User ${userOutbound.get.email.getOrElse("")} created").withHeaders(util.headers: _*)
                  }

                }

              }

            }

          } getOrElse {

            Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "last name not defined"))).withHeaders(util.headers: _*))
          }

        } getOrElse {

          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "first name not defined"))).withHeaders(util.headers: _*))
        }

      } getOrElse {

        Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "email not defined"))).withHeaders(util.headers: _*))
      }

    } getOrElse {

      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "not well-formed"))).withHeaders(util.headers: _*))
    }
  }
}
