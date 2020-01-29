package controllers

import javax.inject.Inject
import play.api.db.Database
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.MetricsService
import utilities.Util

import scala.concurrent.ExecutionContext

class LanguageSupportController @Inject()(cc: ControllerComponents)
                                         (implicit context: ExecutionContext,
                                          database: Database,
                                          metricsService: MetricsService,
                                          messagesApi: MessagesApi,
                                          util: Util) extends AbstractController(cc) with I18nSupport  {
  def index = Action { request =>
    Ok(util.languageSupport(messagesApi, "welcome.index", "usuario")(request))
  }
}