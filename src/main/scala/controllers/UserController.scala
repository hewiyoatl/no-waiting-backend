package controllers

import formatter.{Error, ErrorFormatter}
import javax.inject.Inject
import models.{UserIn, Users}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}


class UserController @Inject()(cc: ControllerComponents, users: Users, config: Configuration, util: Util)
                              (implicit context: ExecutionContext) extends AbstractController(cc) {

  implicit val errorWriter = ErrorFormatter.errorWriter

  def profile = Action.async { request =>

    request.headers.get(HeaderNames.AUTHORIZATION) map { basicHeader =>

      val (user, password) = util.decodeBasicAuth(basicHeader)

      users.retrieveUser(user, password) map { userOpt =>
        //todo: check type of user admin or regular user. and return token

        userOpt map { user =>

          val token = util.provideToken(user)

          Ok(token).withHeaders(util.headers: _*)

        } getOrElse (Forbidden(Json.toJson(Error(FORBIDDEN, "Unauthorized user")))
          .withHeaders(util.headers: _*))

      }

    } getOrElse {

      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Missing authorization header")))
        .withHeaders(util.headers: _*))
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

            users.addUser(newUser) map { userOutbound =>

              Ok("good")

            }

          } getOrElse {

            Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "first name not defined"))))
          }

        } getOrElse {

          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "first name not defined"))))
        }

      } getOrElse {

        Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "not email defined"))))
      }

    } getOrElse {

      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "not well-formed"))))
    }
  }

}
