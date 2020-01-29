package controllers

import auth.AuthService
import formatter.{Error, ErrorFormatter}
import javax.inject.Inject
import models.{UserIn, Users}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import services.{MailerService, MetricsService}
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class UserController @Inject()(cc: ControllerComponents,
                               users: Users,
                               authService: AuthService,
                               config: Configuration,
                               metricsService: MetricsService,
                               mailerService: MailerService,
                               util: Util)
                              (implicit context: ExecutionContext) extends AbstractController(cc) {

  implicit val errorWriter = ErrorFormatter.errorWriter

  def profile = Action.async { request =>

    request.headers.get(HeaderNames.AUTHORIZATION) map { basicHeader =>

      val (user, passwordPlain) = authService.decodeBasicAuth(basicHeader)

      //val password = util.getSha256(passwordPlain)

      users.retrieveUser(user, passwordPlain) map { userOpt =>
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

  def verifyEmail(verifyToken: String) = Action.async { implicit request =>

      authService.validateEmailJwt(verifyToken) match {
        case Success(claim) => {
          val emailToValidate: String = (Json.parse(claim.content) \ "email").as[String]
          users.verifyUser(emailToValidate).flatMap { userExist =>
            userExist match {
              case true => {
                //users.addUser()
                //here made the update
                Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"User already exists"))).withHeaders(util.headers: _*))
              }
              case false => {
                Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"Email do not exists or invalid"))).withHeaders(util.headers: _*))
              }
              case _ => {
                Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"Something strange happened"))).withHeaders(util.headers: _*))
              }
            }
          }
        }
        case Failure(t) => Future.successful(Results.Unauthorized(t.getMessage).withHeaders(util.headers: _*)) // token was invalid - return 401
      }
  }

  def addUser = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[JsValue] = body.asJson

    urlEncodedBody.map { json =>

      val emailOpt: Option[String] = (json \ "email").asOpt[String].filter(_ != "")
      val passwordOpt: Option[String] = (json \ "password").asOpt[String]
      val nicknameOpt: Option[String] = (json \ "nickname").asOpt[String]
      val firstNameOpt: Option[String] = (json \ "first_name").asOpt[String]
      val lastNameOpt: Option[String] = (json \ "last_name").asOpt[String]
      val phoneOpt: Option[String] = (json \ "phone").asOpt[String]

      emailOpt.map { email =>

        firstNameOpt.map { firstName =>

          lastNameOpt.map { lastName =>

            val newUser = UserIn(None, email, nicknameOpt, passwordOpt.getOrElse(""), firstName, lastName, phoneOpt, "Client")

            users.verifyUser(email).flatMap { userExist =>

              userExist match {

                case true => {

                  Future(BadRequest(Json.toJson(Error(BAD_REQUEST, s"User ${email} already exists"))).withHeaders(util.headers: _*))
                }

                case false => {

                  users.addUser(newUser) map { userOutbound =>

                    val emailToken: String = authService.provideTokenEmailVerification(newUser)

                    val emailTokenEncoded: String = URLEncoder.encode(emailToken, StandardCharsets.UTF_8.toString)

                    mailerService.sendVerifyEmail(
                      "Talachitas.com - Verify Email " + firstName + " " + lastName,
                      "talachitasus@gmail.com",
                      email,
                      "mauricio.gomez.77@gmail.com",
                      firstName + " " + lastName + ", you have received successfully your verify email.",
                      "/talachitas/v1/users/verifyEmail/" + emailTokenEncoded
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
