package controllers

import helpers.Auth0Config
import javax.inject.Inject
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.{MetricsService}

import scala.concurrent.{ExecutionContext, Future}


class Callback @Inject()(cc: ControllerComponents)
                        (implicit context: ExecutionContext,
                         config: Configuration,
                         metricsService: MetricsService,
                         wsClient: WSClient,
                         env: play.api.Environment,
                         cache: SyncCacheApi) extends AbstractController(cc) {

  def callback(codeOpt: Option[String] = None) = Action.async {
    (for {
      code <- codeOpt
    } yield {
        getToken(code).flatMap { case (idToken, accessToken) =>
          getUser(accessToken).map { user =>
            cache.set(idToken + "profile", user)
            Redirect(routes.User.index())
              .withSession(
                "idToken" -> idToken,
                "accessToken" -> accessToken
              )
          }

        }.recover {
          case ex: IllegalStateException => Unauthorized(ex.getMessage)
        }
      }).getOrElse(Future.successful(BadRequest("No parameters supplied")))
  }

  def getToken(code: String): Future[(String, String)] = {
    val authConfig = Auth0Config.get(config)
    val tokenResponse =
      wsClient.url(String.format("https://%s/oauth/token", authConfig.domain))
        .addHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
        .post(
        Json.obj(
          "client_id" -> authConfig.clientId,
          "client_secret" -> authConfig.secret,
          "redirect_uri" -> authConfig.callbackURL,
          "code" -> code,
          "grant_type" -> "authorization_code"
        )
      )

    tokenResponse.flatMap { response =>
      (for {
        idToken <- (response.json \ "id_token").asOpt[String]
        accessToken <- (response.json \ "access_token").asOpt[String]
      } yield {
          Future.successful((idToken, accessToken))
        }).getOrElse(Future.failed[(String, String)](new IllegalStateException("Tokens not sent")))
    }

  }

  def getUser(accessToken: String): Future[JsValue] = {
    val authConfig = Auth0Config.get(config)
    val userResponse = wsClient.url(String.format("https://%s/userinfo", authConfig.domain))
      .addQueryStringParameters("access_token" -> accessToken)
      .get()

    userResponse.flatMap(response => Future.successful(response.json))
  }
}
