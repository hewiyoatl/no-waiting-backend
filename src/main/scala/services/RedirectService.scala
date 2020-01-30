package services

import javax.inject.Inject
import play.api.Configuration

class RedirectService @Inject() (config: Configuration) {

  val talachitasDomainFrontEnd: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.home.frontend.app.local")
    case "prod" => config.get[String]("talachitas.home.frontend.app.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.home.frontend.app'")
  }

  final val talachitasDomainBackend: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.home.backend.app.protocol.local") + config.get[String]("talachitas.home.backend.app.local")
    case "prod" => config.get[String]("talachitas.home.backend.app.protocol.prod") + config.get[String]("talachitas.home.backend.app.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.home.backend.app.protocal'")
  }

  val DEFAULT_LANGUAGE = "en"
  val DEFAULT_APP_OR_STATIC = "static"

  val KEY_SUCCESS_PAGE = "success-message.html"
  val KEY_FAILURE_PAGE = "error-message.html"
  val KEY_INFO_PAGE    = "info-message.html"

  val ENGLISH_DOMAIN_STATIC = talachitasDomainFrontEnd + "talachitas/html/english/"
  val SPANISH_DOMAIN_STATIC = talachitasDomainFrontEnd + "talachitas/html/spanish/"

  val ENGLISH_DOMAIN_APP = talachitasDomainFrontEnd +  "talachitas/html/english/app/"
  val SPANISH_DOMAIN_APP = talachitasDomainFrontEnd +  "talachitas/html/spanish/app/"

  private case class ParamsUI(languageOpt: Option[String], appOrStaticOpt: Option[String])

  def redirect(languageOpt: Option[String], appOrStaticOpt: Option[String], key: String): String = {

    ParamsUI(languageOpt, appOrStaticOpt) match {

      case ParamsUI(None, None) => ENGLISH_DOMAIN_STATIC + key
      case ParamsUI(None, _)    => ENGLISH_DOMAIN_STATIC + key
      case ParamsUI(_, None)    => ENGLISH_DOMAIN_STATIC + key

      case ParamsUI(Some("en"), Some("static")) => ENGLISH_DOMAIN_STATIC + key
      case ParamsUI(Some("en"), Some("app"))    => ENGLISH_DOMAIN_APP + key

      case ParamsUI(Some("sp"), Some("static")) => SPANISH_DOMAIN_STATIC + key
      case ParamsUI(Some("sp"), Some("app"))    => SPANISH_DOMAIN_APP    + key
    }
  }
}
