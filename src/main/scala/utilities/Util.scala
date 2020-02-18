package utilities

import com.kenshoo.play.metrics.Metrics
import com.zaxxer.hikari.HikariDataSource
import javax.inject.Inject
import play.api.db.Database
import play.api.{Configuration, Logger}

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

  def headersCors = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT, PATCH",
    "Access-Control-Max-Age" -> "3600",
    "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization, Access-Control-Allow-Methods, Access-Control-Allow-Origin, Access-Control-Allow-Headers",
    "Access-Control-Allow-Credentials" -> "true"
  )

  def initializeDBMetrics(db: Database, metrics: Metrics): Unit = {
    val dataSource : HikariDataSource = db.dataSource.asInstanceOf[HikariDataSource]
    if (dataSource.getMetricRegistry() == null) {
      dataSource.setMetricRegistry(metrics.defaultRegistry)
    }
  }

  def lines = scala.io.Source.fromFile("/opt/resources/play.properties").getLines().mkString

  case class URLParts(urlProtocol: String, urlHost: String, urlPort: Int, urlPath: String)

  def getUrlParts(url: String): URLParts = {
    val urlExtract = new java.net.URL(url)
    URLParts(urlExtract.getProtocol, urlExtract.getHost, urlExtract.getPort, urlExtract.getPath)
  }

}
