package controllers

import javax.inject.Inject
import models.Contacts
import play.api.mvc._
import services.{MetricsService}
import utilities.Util

import scala.concurrent.ExecutionContext


class OptionsController @Inject()(cc: ControllerComponents, contactss: Contacts)
                                 (implicit context: ExecutionContext,
                                  metricsService: MetricsService,
                                  util: Util) extends AbstractController(cc) {

  def options = Action {
    NoContent.withHeaders(util.headersCors : _*)
  }

  def optionsString(email: String) = Action {
    NoContent.withHeaders(util.headersCors : _*)
  }

  def optionsLong(id: Long) = Action {
    NoContent.withHeaders(util.headersCors : _*)
  }
}
