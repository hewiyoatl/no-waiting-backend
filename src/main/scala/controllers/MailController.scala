package controllers

import auth.{AuthAdminAction, AuthUserAction}
import javax.inject.Inject
import play.api.mvc._
import play.api.{Configuration, Logger}
import modules.MailerService
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MailController @Inject()(cc: ControllerComponents)
                              (implicit context: ExecutionContext,
                               metrics: MetricsFacade,
                               authUserAction: AuthUserAction,
                               authAdminAction: AuthAdminAction,
                               config: Configuration,
                               mailService: MailerService,
                               util: Util) extends AbstractController(cc) {


  val logger: Logger = Logger(this.getClass())

  def testEmail = Action.async { implicit request =>

    val currentDirectory = new java.io.File(".").getCanonicalPath

    ///Users/mauriciodanielgomez_torres/code/no-waiting-backend
    logger.info("current directory: " + currentDirectory)

    val emailTemplate: String = scala.io.Source.fromFile(currentDirectory + "/src/main/resources/emailTemplates/" + "emailVerification.html").getLines().mkString

    val replaceVariables = emailTemplate.replaceAll("<codeMessage></codeMessage>", "This is your email for verification")
    mailService.sendEmailHtml("Verification Email", "talachitasus@gmail.com", Seq("mauricio.gomez.77@gmail.com"),  replaceVariables);
    Future(Ok("Email sent!"))
  }
}
