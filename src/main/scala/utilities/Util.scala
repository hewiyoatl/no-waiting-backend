package utilities

import com.kenshoo.play.metrics.Metrics
import com.zaxxer.hikari.HikariDataSource
import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.db.Database
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc._

class Util @Inject()(config: Configuration) {

  val logger: Logger = Logger(this.getClass())

  /**
   * This variable is used to check the request param
   */
  val PRETTY = "pretty"

  /**
   * Empty json
   */
  val EMPTY_JSON: String = "{}"

  /**
   * Utility method to validate that all the characters of a string are digits.
   *
   * @param value String to validate.
   * @return True if all characters are digits, false otherwise.
   */
  def isDigit(value: String): Boolean = value forall Character.isDigit

  /**
   * Related issues: https://groups.google.com/forum/#!topic/swagger-swaggersocket/TD0MWY09ESo
   * seems play filters still is in the works
   *
   * @param result
   * @return
   */
  def enableCors(result: Result) = result.withHeaders(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "PATCH, OPTIONS, GET, POST, PUT, DELETE, HEAD",
    "Access-Control-Allow-Headers" -> "Referrer, User-Agent, Cache-Control, Pragma, Date, Authorization, api_key, Accept, Content-Type, Origin, X-Json, X-Prototype-Version, X-Requested-With",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Expose-Headers" -> "WWW-Authenticate, Server-Authorization, Location"
  )

  def initializeDBMetrics(db: Database, metrics: Metrics): Unit = {
    val dataSource : HikariDataSource = db.dataSource.asInstanceOf[HikariDataSource]
    if (dataSource.getMetricRegistry() == null) {
      dataSource.setMetricRegistry(metrics.defaultRegistry)
    }
  }

  def languageSupport(messagesApi: MessagesApi,
                      messageCode: String, param1: String)
                     (implicit request: RequestHeader): String = {
    val language: String =  request.acceptLanguages.map(_.code).headOption.getOrElse("en")
    messagesApi(messageCode, param1)(Lang(language))
  }

  def lines = scala.io.Source.fromFile("/opt/resources/play.properties").getLines().mkString

  case class URLParts(urlProtocol: String, urlHost: String, urlPort: Int, urlPath: String)

  def getUrlParts(url: String): URLParts = {
    val urlExtract = new java.net.URL(url)
    URLParts(urlExtract.getProtocol, urlExtract.getHost, urlExtract.getPort, urlExtract.getPath)
  }

  def headers = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
    "Access-Control-Max-Age" -> "3600",
    "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization, access-control-allow-methods, access-control-allow-origin, access-control-allow-headers",
    "Access-Control-Allow-Credentials" -> "true"
  )

}
