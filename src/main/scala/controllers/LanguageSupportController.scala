package controllers

import javax.inject.Inject
import play.api.mvc._
import services.{CustomizedLanguageService, MetricsService}

import scala.concurrent.ExecutionContext

class CustomizedLanguageController @Inject()(cc: ControllerComponents)
                                            (implicit context: ExecutionContext,
                                             metricsService: MetricsService,
                                             customizedLanguageService: CustomizedLanguageService)
  extends AbstractController(cc) {


  def language(language: String, messageKey: String) = Action {
    Ok(customizedLanguageService.customizedLanguageMessage(language, messageKey, ""))
  }


  def allMessages = Action {
    Ok(customizedLanguageService.allMessages)
  }
}