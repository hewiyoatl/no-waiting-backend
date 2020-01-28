package controllers

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, _}
import play.api.cache.SyncCacheApi
import services.{MetricsService}

import scala.concurrent.ExecutionContext

class User @Inject()(cc: ControllerComponents)
                    (implicit context: ExecutionContext,
                     config: Configuration,
                     metricsService: MetricsService,
                     wsClient: WSClient,
                     env: play.api.Environment,
                     cache: SyncCacheApi) extends AbstractController(cc) {

  def AuthenticatedAction(f: Request[AnyContent] => Result): Action[AnyContent] = {

    Action { request =>
      (request.session.get("idToken").flatMap { idToken =>
        cache.get[JsValue](idToken + "profile")
      } map { profile =>
        f(request)
      }).orElse {
        Some(Redirect(routes.Application.index()))
      }.get
    }
  }

  def index = AuthenticatedAction { request =>
    val idToken = request.session.get("idToken").get
    val profile = cache.get[JsValue](idToken + "profile").get
    Ok(views.html.user(profile))
  }
}
