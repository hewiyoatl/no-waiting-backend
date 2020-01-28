package services

import java.util
import java.util.Collections

import com.codahale.metrics.Timer
import com.kenshoo.play.metrics._
import io.prometheus.client._
import javax.inject.Inject

case class MetricsResponse(histogram: Histogram.Timer,
                           summary: Summary.Timer,
                           context: Timer.Context,
                           http: String,
                           method: String)

/**
 * Class to help for the metrics objects
 *
 * @param metrics
 */
class MetricsService @Inject()(metrics: Metrics){

  val requests: Counter = Counter.build()
    .name("requests_total").help("Total requests.").labelNames("http", "function").register()

  val requestLatency: Summary = Summary.build()
    .name("requests_latency_seconds").help("Request latency in seconds.").labelNames("http", "function").register()

  val inprogressRequests: Gauge = Gauge.build()
    .name("inprogress_requests").help("Inprogress requests.").labelNames("http", "function").register()

  val requestLatencyHistogram: Histogram = Histogram.build()
    .name("requests_latency_seconds").help("Request latency in seconds.").labelNames("http", "function").register()

  def registerCounter(http: String, methodName: String): Unit =
    requests.labels(http, methodName).inc()

  def startGauge(http: String, methodName: String): Unit =
    inprogressRequests.labels(http, methodName).inc()

  def stopGauge(http: String, methodName: String): Unit =
    inprogressRequests.labels(http, methodName).dec()

  def registerSummary(http: String, methodName: String): Summary.Timer =
    requestLatency.labels(http, methodName).startTimer()

  def registerHistogram(http: String, methodName: String):  Histogram.Timer =
    requestLatencyHistogram.labels(http, methodName).startTimer()

  def metricsMeasureStart(http: String, methodName: String): MetricsResponse = {
    registerCounter(http, methodName)
    startGauge(http, methodName)
    val histogram: Histogram.Timer = registerHistogram(http, methodName)
    val summary: Summary.Timer = registerSummary(http, methodName)
    val timer: Timer = metricName(methodName)
    val context: Timer.Context = timer.time()
    MetricsResponse(histogram, summary, context, http, methodName)
  }

  def metricsMeasureEnd(metricsResponse: MetricsResponse): Unit = {
    stopGauge(metricsResponse.http, metricsResponse.method)
    metricsResponse.context.stop()
    metricsResponse.summary.observeDuration()
    metricsResponse.histogram.observeDuration()
  }

  def metricName(methodName: String) = metrics.defaultRegistry.timer(methodName): Timer

  def metricsPlay = metrics

  import scala.collection.JavaConversions._

  def write004(mfs: util.Enumeration[Collector.MetricFamilySamples]): String = {

    val writer: StringBuilder = new StringBuilder
    for (metricFamilySamples <- Collections.list(mfs)) {
      writer.append("# HELP " + metricFamilySamples.name + " " + escapeHelp(metricFamilySamples.help) + "\n")
      writer.append("# TYPE " + metricFamilySamples.name + " " + typeString(metricFamilySamples.`type`) + "\n")
      for (sample <- metricFamilySamples.samples) {
        writer.append(sample.name)
        if (sample.labelNames.size > 0) {
          writer.append("{")
          for (i <- 0 until sample.labelNames.size()) {
            //for (int i = 0; i < sample.labelNames.size(); ++i) {
            writer.append(String.format("%s=\"%s\",", sample.labelNames.get(i), escapeLabelValue(sample.labelValues.get(i))))
          }
          writer.append("}")
        }
        writer.append(" " + Collector.doubleToGoString(sample.value) + "\n")
      }
    }
    writer.toString()
  }

  def escapeHelp(s: String): String = {
    s.replace("\\", "\\\\").replace("\n", "\\n")
  }
  def escapeLabelValue(s: String): String = {
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  def typeString(t: Collector.Type): String = {
    t match {
      case Collector.Type.GAUGE => "gauge"
      case Collector.Type.COUNTER => "counter"
      case Collector.Type.SUMMARY => "summary"
      case Collector.Type.HISTOGRAM => "histogram"
      case _ => "untyped"
    }
  }
}
