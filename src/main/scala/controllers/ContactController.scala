package controllers

import auth.{AuthAdminAction, AuthUserAction}
import formatter._
import javax.inject.Inject
import models.{ContactTable, Contacts}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import utilities.Util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class ContactController @Inject()(cc: ControllerComponents, contactss: Contacts)
                                 (implicit context: ExecutionContext,
                                  metrics: MetricsFacade,
                                  authUserAction: AuthUserAction,
                                  authAdminAction: AuthAdminAction,
                                  config: Configuration,
                                  util: Util) extends AbstractController(cc) {

  val DEFAULT_LANGUAGE = "en"
  val DEFAULT_APP_OR_STATIC = "static"

  val KEY_SUCCESS_PAGE = "success-message.html"
  val KEY_FAILURE_PAGE = "error-message.html"
  val KEY_INFO_PAGE    = "info-message.html"

  val ENGLISH_DOMAIN_STATIC = "https://www.talachitas.com/talachitas/html/english/"
  val SPANISH_DOMAIN_STATIC = "https://www.talachitas.com/talachitas/html/spanish/"

  val ENGLISH_DOMAIN_APP = "https://www.talachitas.com/talachitas/html/english/app/"
  val SPANISH_DOMAIN_APP = "https://www.talachitas.com/talachitas/html/spanish/app/"

  implicit val contactReader = ContactFormatter.ContactReader

  implicit val contactWriter = ContactFormatter.ContactWriter

  implicit val errorWriter = ErrorFormatter.errorWriter

  def ping = authUserAction.async { implicit request =>

    Future(Ok("Hello, Scala!"))
  }

  def listContacts = authAdminAction.async { implicit request =>

    contactss.listContacts map { contacts =>

      Ok(Json.toJson(contacts)).withHeaders(util.headers: _*)
    }

  }

  case class ParamsUI(languageOpt: Option[String], appOrStaticOpt: Option[String])

  private def redirect(languageOpt: Option[String], appOrStaticOpt: Option[String], key: String): Result = {
    ParamsUI(languageOpt, appOrStaticOpt) match {
      case ParamsUI(None, None) => Redirect(ENGLISH_DOMAIN_STATIC + key)
      case ParamsUI(None, _)    => Redirect(ENGLISH_DOMAIN_STATIC + key)
      case ParamsUI(_, None)    => Redirect(ENGLISH_DOMAIN_STATIC + key)

      case ParamsUI(Some("en"), Some("static")) => Redirect(ENGLISH_DOMAIN_STATIC + key)
      case ParamsUI(Some("en"), Some("app"))    => Redirect(ENGLISH_DOMAIN_APP + key)

      case ParamsUI(Some("sp"), Some("static")) => Redirect(SPANISH_DOMAIN_STATIC + key)
      case ParamsUI(Some("sp"), Some("app"))    => Redirect(SPANISH_DOMAIN_APP    + key)
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
      val languageOpt = encodedBody.get("language").map(_.mkString).orElse(Some(DEFAULT_LANGUAGE))
      val appOrStaticOpt = encodedBody.get("app-static").map(_.mkString).orElse(Some(DEFAULT_APP_OR_STATIC))

      emailOpt.map { email =>

        val contactUser = ContactTable(
          emailOpt, subjectOpt, messageOpt, phoneNumberOpt)

        contactss.add(contactUser) map { contactOutbound =>

          redirect(languageOpt, appOrStaticOpt, KEY_SUCCESS_PAGE)

        }

      } getOrElse {

        Future(redirect(languageOpt, appOrStaticOpt, KEY_FAILURE_PAGE))
      }

    } getOrElse {

      Future(redirect(Some(DEFAULT_LANGUAGE), Some(DEFAULT_APP_OR_STATIC), KEY_FAILURE_PAGE))
    }
  }

  def deleteContact(email: String) = authAdminAction.async { implicit request =>

    contactss.deleteContact(email)
    Future(NoContent.withHeaders(util.headers: _*))
  }

}
