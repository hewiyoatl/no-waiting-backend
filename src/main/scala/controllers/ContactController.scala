package controllers

import auth.{AuthAdminAction, AuthUserAction}
import formatter._
import javax.inject.Inject
import models.{ContactTable, Contacts}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import services.{CustomizedLanguageService, LanguageAction, MetricsService, RedirectService}
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class ContactController @Inject()(cc: ControllerComponents, contactss: Contacts)
                                 (implicit context: ExecutionContext,
                                  metricsService: MetricsService,
                                  authUserAction: AuthUserAction,
                                  authAdminAction: AuthAdminAction,
                                  languageAction: LanguageAction,
                                  config: Configuration,
                                  redirectService: RedirectService,
                                  interMessage: CustomizedLanguageService,
                                  util: Util) extends AbstractController(cc) {

  implicit val contactReader = ContactFormatter.ContactReader

  implicit val contactWriter = ContactFormatter.ContactWriter

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  implicit val successWriter = SuccessMessageFormatter.successWriter

  def ping = languageAction.andThen(authUserAction).async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    Future(Ok("Hello, Scala! " + language))
  }

  def listContacts = languageAction.andThen(authAdminAction).async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    contactss.listContacts map { contacts =>
      val listContacts:Option[String] = Some(Json.stringify(Json.toJson(contacts)))
      val message: String = interMessage.customizedLanguageMessage(language, "contact.list.success", "")
      Ok(Json.toJson(SuccessMessage(OK, message, listContacts))).withHeaders(util.headersCors: _*)
    }
  }

  def addContact = languageAction.async { implicit request =>

    val language: String = request.acceptLanguages.head.code
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

  def deleteContact(email: String) = languageAction.andThen(authAdminAction).async { implicit request =>
    val language: String = request.acceptLanguages.head.code
    contactss.deleteContact(email)
    val message: String = interMessage.customizedLanguageMessage(language, "contact.delete.success", "")
    Future(Ok(Json.toJson(SuccessMessage(OK, message, None))).withHeaders(util.headersCors: _*))
  }
}
