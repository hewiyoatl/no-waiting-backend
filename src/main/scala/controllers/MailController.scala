package controllers

import auth.{AuthAdminAction, AuthUserAction}
import javax.inject.Inject
import play.api.mvc._
import play.api.{Configuration, Logger}
import services.{MailerService, MetricsService}
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}

class MailController @Inject()(cc: ControllerComponents)
                              (implicit context: ExecutionContext,
                               metricsService: MetricsService,
                               authUserAction: AuthUserAction,
                               authAdminAction: AuthAdminAction,
                               config: Configuration,
                               mailService: MailerService,
                               util: Util) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass())

  def testEmail = Action.async { implicit request =>

    Future(Ok("Email sent ! " +
      mailService.sendVerifyEmail(
        "subject",
        "talachitasus@gmail.com",
        "mauricio.gomez.77@gmail.com",
        "mauricio.gomez.77@gmail.com",
        "welcome message",
        "link?123123")))
  }
}
