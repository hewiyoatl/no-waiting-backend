package controllers

import javax.inject.Inject
import formatter.{Error, ErrorFormatter, UserFormatter, UserInbound}
import model.Clients
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.MetricsService

import scala.concurrent.{ExecutionContext, Future}

class ClientController @Inject()(cc: ControllerComponents, users: Clients)
                                (implicit context: ExecutionContext,
                                 config: Configuration,
                                 metricsService: MetricsService,
                                 wsClient: WSClient,
                                 env: play.api.Environment,
                                 cache: SyncCacheApi) extends AbstractController(cc) {

  implicit val userReader = UserFormatter.UserReader

  implicit val userWriter = UserFormatter.UserWriter

  implicit val errorWriter = ErrorFormatter.errorWriter

  def listUsers = Action.async { implicit request =>
    users.listAll map { users =>

      Ok(Json.toJson(users))
    }

  }

  def addUser = Action.async { implicit request =>

    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[UserInbound] = json.validate[UserInbound]

        resultVal.asOpt.map { userInboud =>

          val newUser = model.Client(
            None,
            userInboud.firstName,
            userInboud.lastName,
            userInboud.mobile,
            userInboud.email,
            false)

          users.add(newUser) map { user =>

            Created(Json.toJson(user))
          }
        } getOrElse {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "new error"))))
        }
    } getOrElse {
      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Error"))))
    }
  }

  def deleteUser(id: Long) = Action.async { implicit request =>
    users.delete(id)
    Future(NoContent)
  }

  def retrieveUser(id: Long) = Action.async { implicit request =>
    users.retrieveClient(id) map { user =>

      Ok(Json.toJson(user))
    }
  }

  def patchUser(id: Long) = Action.async { implicit request =>

    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[UserInbound] = json.validate[UserInbound]

        resultVal.asOpt.map { userInboud =>

          val patchUser = model.Client(
            Some(id),
            userInboud.firstName,
            userInboud.lastName,
            userInboud.mobile,
            userInboud.email,
            false)

          users.patchClient(patchUser) map { user =>

            Ok(Json.toJson(user))
          }
        } getOrElse {
          Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "new error"))))
        }
    } getOrElse {
      Future(BadRequest(Json.toJson(Error(BAD_REQUEST, "Error"))))
    }
  }

}
