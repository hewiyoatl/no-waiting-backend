package controllers

import helpers.Auth0Config
import javax.inject.Inject
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents}
import services.MetricsService

import scala.concurrent.ExecutionContext

class Application @Inject()(cc: ControllerComponents)
                           (implicit context: ExecutionContext,
                            config: Configuration,
                            metricsService: MetricsService,
                            wsClient: WSClient,
                            env: play.api.Environment,
                            cache: SyncCacheApi) extends AbstractController(cc) {

  def index = Action {
    Ok(views.html.index(Auth0Config.get(config)))
  }
}
