package controllers

import auth.{AuthAdminAction, AuthUserAction}
import formatter._
import javax.inject.Inject
import models.{ContactTable, Contacts}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.{MetricsService, RedirectService}
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class ContactController @Inject()(cc: ControllerComponents, contactss: Contacts)
                                 (implicit context: ExecutionContext,
                                  metricsService: MetricsService,
                                  authUserAction: AuthUserAction,
                                  authAdminAction: AuthAdminAction,
                                  config: Configuration,
                                  redirectService: RedirectService,
                                  util: Util) extends AbstractController(cc) {

  implicit val contactReader = ContactFormatter.ContactReader

  implicit val contactWriter = ContactFormatter.ContactWriter

  implicit val errorWriter = ErrorFormatter.errorWriter

  def ping = authUserAction.async { implicit request =>

    Future(Ok("Hello, Scala!"))
  }

  def listContacts = authAdminAction.async { implicit request =>

    contactss.listContacts map { contacts =>

      Ok(Json.toJson(contacts)).withHeaders(util.headersCors: _*)
    }

  }

  def addContact = Action.async { implicit request =>

    val body: AnyContent = request.body
    val urlEncodedBody: Option[Map[String, Seq[String]]] = body.asFormUrlEncoded

    urlEncodedBody.map { encodedBody =>
      // we do not accept empty string
      val emailOpt = encodedBody.get("email").map(_.mkString).filter(_ != "")
      val subjectOpt = encodedBody.get("subject").map(_.mkString)
      val messageOpt = encodedBody.get("message").map(_.mkString)
      val phoneNumberOpt = encodedBody.get("phone").map(_.mkString)
      val languageOpt = encodedBody.get("language").map(_.mkString).orElse(Some(redirectService.DEFAULT_LANGUAGE))
      val appOrStaticOpt = encodedBody.get("app-static").map(_.mkString).orElse(Some(redirectService.DEFAULT_APP_OR_STATIC))

      emailOpt.map { email =>

        val contactUser = ContactTable(
          emailOpt, subjectOpt, messageOpt, phoneNumberOpt)

        contactss.add(contactUser) map { contactOutbound =>
          Redirect(redirectService.redirect(languageOpt, appOrStaticOpt, redirectService.KEY_SUCCESS_PAGE))
        }

      } getOrElse {

        Future(Redirect(redirectService.redirect(languageOpt, appOrStaticOpt, redirectService.KEY_FAILURE_PAGE)))
      }

    } getOrElse {

      Future(Redirect(redirectService.redirect(Some(redirectService.DEFAULT_LANGUAGE), Some(redirectService.DEFAULT_APP_OR_STATIC), redirectService.KEY_FAILURE_PAGE)))
    }
  }

  def deleteContact(email: String) = authAdminAction.async { implicit request =>

    contactss.deleteContact(email)
    Future(NoContent.withHeaders(util.headersCors: _*))
  }

}
