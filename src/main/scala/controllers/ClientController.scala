package controllers

import auth.AuthAdminAction
import formatter._
import javax.inject.Inject
import model.Clients
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.{CustomizedLanguageService, LanguageAction, MetricsService}
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}

class ClientController @Inject()(cc: ControllerComponents, users: Clients)
                                (implicit context: ExecutionContext,
                                 config: Configuration,
                                 metricsService: MetricsService,
                                 wsClient: WSClient,
                                 env: play.api.Environment,
                                 cache: SyncCacheApi,
                                 interMessage: CustomizedLanguageService,
                                 languageAction: LanguageAction,
                                 authAdminAction: AuthAdminAction,
                                 util: Util) extends AbstractController(cc) {

  implicit val userReader = UserFormatter.UserReader

  implicit val userWriter = UserFormatter.UserWriter

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

//      def listUsers = languageAction.andThen(authAdminAction).async {
  def listUsers = languageAction.async {  implicit request =>

    val language: String = request.acceptLanguages.head.code

    users.listAll map { users =>
      val listUsers:Option[String] = Some(Json.stringify(Json.toJson(users)))
//      val successResponse: JsObject = Json.toJson(Success(OK, "List of users", tt)).as[JsObject]
      //https://stackoverflow.com/questions/17596809/how-to-merge-a-jsvalue-to-jsobject-in-flat-level/36795978
      //val usersResponse: JsObject = Json.toJson(users).as[JsObject]
//      val usersResponse:JsArray = Json.toJson(users).as[JsArray]
//      val asSeqOfJsObjects:Seq[JsObject] = usersResponse.value.map(_.as[JsObject]) ++ Seq(successResponse)
      val message: String = interMessage.customizedLanguageMessage(language, "client.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listUsers))).withHeaders(util.headersCors: _*)
    }
  }

  def addUser = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code

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
            val userString:Option[String] = Some(Json.stringify(Json.toJson(user)))
            val message: String = interMessage.customizedLanguageMessage(language, "client.creation.success", "")
            Created(Json.toJson(SuccessMessage(OK, message, userString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "client.creation.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "client.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def deleteUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    users.delete(id)
    val message: String = interMessage.customizedLanguageMessage(language, "client.delete.success", "")
    Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
  }

  def retrieveUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    users.retrieveClient(id) map { user =>
      val userString:Option[String] = Some(Json.stringify(Json.toJson(user)))
      val message: String = interMessage.customizedLanguageMessage(language, "client.retrieve.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, userString))).withHeaders(util.headersCors: _*)
    }
  }

  def patchUser(id: Long) = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
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
            val userString:Option[String] = Some(Json.stringify(Json.toJson(user)))
            val message: String = interMessage.customizedLanguageMessage(language, "client.update.success", "")
            Ok(Json.toJson(SuccessMessage(OK, message, userString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "client.update.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "client.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
    }
  }
}
