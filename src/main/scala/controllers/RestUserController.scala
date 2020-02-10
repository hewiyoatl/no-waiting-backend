package controllers

import formatter._
import javax.inject.Inject
import models.{RestUser, RestUsers}
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.{CustomizedLanguageService, LanguageAction}
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}

class RestUserController @Inject()(cc: ControllerComponents, restUsers: RestUsers)
                                  (implicit context: ExecutionContext,
                                   interMessage: CustomizedLanguageService,
                                   languageAction: LanguageAction,
                                   util: Util) extends AbstractController(cc) {

  implicit val RestUserReader = RestUserFormatter.RestUserReader

  implicit val RestUserWriter = RestUserFormatter.RestUserWriter

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

  def listRestUsers = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    restUsers.listAll map { restUsers =>
      val listRestUsers:Option[String] = Some(Json.stringify(Json.toJson(restUsers)))
      val message: String = interMessage.customizedLanguageMessage(language, "restuser.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listRestUsers))).withHeaders(util.headersCors: _*)
    }
  }

  def addRestUser = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[RestUserInbound] = json.validate[RestUserInbound]

        resultVal.asOpt.map { userInboud =>

          val newRestUser = RestUser(
            None,
            userInboud.sucursalId,
            userInboud.firstName,
            userInboud.lastName,
            userInboud.mobile,
            userInboud.userName,
            userInboud.password,
            false)

          restUsers.add(newRestUser) map { restUser =>
            val restUserString:Option[String] = Some(Json.stringify(Json.toJson(restUser)))
            val message: String = interMessage.customizedLanguageMessage(language, "restuser.creation.success", "")
            Created(Json.toJson(SuccessMessage(OK, message, restUserString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "restuser.creation.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "restuser.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))).withHeaders(util.headersCors: _*))
    }
  }

  def deleteRestUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    restUsers.delete(id)
    val message: String = interMessage.customizedLanguageMessage(language, "restuser.delete.success", "")
    Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
  }

  def retrieveRestUser(id: Long) = languageAction.async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    restUsers.retrieveRestUser(id) map { restUser =>
      val restUserString:Option[String] = Some(Json.stringify(Json.toJson(restUser)))
      val message: String = interMessage.customizedLanguageMessage(language, "restuser.retrieve.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, restUserString))).withHeaders(util.headersCors: _*)
    }
  }

  def patchRestUser(id: Long) = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    jsonBody.map {
      json =>

        val resultVal: JsResult[RestUserInbound] = json.validate[RestUserInbound]

        resultVal.asOpt.map { restUserInboud =>

          val patchRestUser = RestUser(
            Some(id),
            restUserInboud.sucursalId,
            restUserInboud.firstName,
            restUserInboud.lastName,
            restUserInboud.mobile,
            restUserInboud.userName,
            restUserInboud.password,
            false)

          restUsers.patchRestUser(patchRestUser) map { restUser =>
            val restUserString:Option[String] = Some(Json.stringify(Json.toJson(restUser)))
            val message: String = interMessage.customizedLanguageMessage(language, "restuser.update.success", "")
            Ok(Json.toJson(SuccessMessage(OK, message, restUserString))).withHeaders(util.headersCors: _*)
          }
        } getOrElse {
          val message: String = interMessage.customizedLanguageMessage(language, "restuser.update.error", "")
          Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
        }
    } getOrElse {
      val message: String = interMessage.customizedLanguageMessage(language, "restuser.body.error", "")
      Future(BadRequest(Json.toJson(ErrorMessage(BAD_REQUEST, message))))
    }
  }
}
