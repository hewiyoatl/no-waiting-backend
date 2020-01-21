package controllers

import javax.inject.Inject
import models.Contacts
import play.api.mvc._
import utilities.Util

import scala.concurrent.ExecutionContext


class OptionsController @Inject()(cc: ControllerComponents, contactss: Contacts)
                                 (implicit context: ExecutionContext,
                                  metrics: MetricsFacade,
                                  util: Util) extends AbstractController(cc) {

  def options = Action { request =>
    NoContent.withHeaders(util.headers : _*)
  }

  def optionsString(email: String) = Action { request =>
    NoContent.withHeaders(util.headers : _*)
  }
}
