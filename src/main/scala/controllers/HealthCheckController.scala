package controllers

import java.util
import java.util.Map.Entry

import com.codahale.metrics.health.HealthCheck.Result
import com.codahale.metrics.health.HealthCheckRegistry
import com.kenshoo.play.metrics.MetricsDisabledException
import healthcheks.DatabaseHealthCheck
import io.prometheus.client.CollectorRegistry
import javax.inject.Inject
import play.api.Logger
import play.api.db._
import play.api.http.ContentTypes
import play.api.mvc._
import services.MetricsService

import scala.concurrent.ExecutionContext

/**
 * Class to provide all the health checks related operations
 *
 * @param database
 * @param metrics
 */
class HealthCheckController @Inject()(cc: ControllerComponents)
                                     (implicit context: ExecutionContext,
                                      database: Database,
                                      metricsService: MetricsService) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass())

  val healthyMessage : String = " is healthy"

  val unhealthyMessage : String = " is not healthy"

  val healthChecks: HealthCheckRegistry = new HealthCheckRegistry

  /**
   * Register all the existing health checks
   */
  def registerHealthchecks : Unit = {
    healthChecks.register("postgres", new DatabaseHealthCheck(database))
  }

  registerHealthchecks

  /**
   * Method to run all the registered health checks
   *
   * @return
   */
  def runAllHealthChecks = Action { request =>
    val results: util.SortedMap[String, Result] = healthChecks.runHealthChecks()
    val iterator: util.Iterator[Entry[String, Result]] = results.entrySet().iterator()
    var healthCheckString : String = ""
    while (iterator.hasNext) {
      val entry: Entry[String, Result] = iterator.next
      if (entry.getValue.isHealthy) {
        logger.info(entry.getKey + healthyMessage)
        healthCheckString += entry.getKey + healthyMessage
      } else {
        val e: Throwable = entry.getValue.getError
        if (e != null) {
          logger.error(entry.getKey + unhealthyMessage + entry.getValue.getMessage, e)
        } else {
          logger.error(entry.getKey + unhealthyMessage + entry.getValue.getMessage)
        }
        healthCheckString += entry.getKey + unhealthyMessage + entry.getValue.getMessage
      }
    }
    Ok(healthCheckString)
  }

  /**
   * Method to capture the current metrics that the system is
   * providing via drop wizard metrics
   *
   * @return
   */
  def captureMetrics = Action {
    try {
      Ok(metricsService.metricsPlay.toJson)
        .as("application/json")
        .withHeaders("Cache-Control" -> "must-revalidate,no-cache,no-store")
    } catch {
      case ex @ (_ : MetricsDisabledException | _ : Exception) =>
        InternalServerError("metrics plugin not enabled")
    }
  }

  /**
   * Method to capture the current metrics that the system is
   * providing via prometheus metrics
   *
   * @return
   */
  def captureMetricsPrometheus = Action {
    val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
    Ok(metricsService.write004(registry.metricFamilySamples))
      .as(ContentTypes.TEXT)
  }

  def pong = Action { request =>
    logger.info("Ping Pong response ")
    Ok("Pong!").as(ContentTypes.TEXT)
  }

}
