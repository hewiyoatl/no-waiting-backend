package services

import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.libs.ws._
import play.mvc.Http.{HeaderNames, MimeTypes}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class SmsService @Inject()(config: Configuration,
                           ws: WSClient) {

  final val talachitasSmsDomainBackend: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.home.backend.app.protocol.local") + config.get[String]("talachitas.home.sms.backend.app.local")
    case "prod" => config.get[String]("talachitas.home.backend.app.protocol.prod") + config.get[String]("talachitas.home.sms.backend.app.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.home.sms.backend.app.'")
  }

  final val talachitasSmsResource: String = "/talachitas/sms/v1/messages"

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  val logger: Logger = Logger(this.getClass())

  def sendMessage(authorization: String, messageType: Int, phoneNumber: String): Unit = {
    try {
      val response: Future[WSResponse] = ws.url(talachitasSmsDomainBackend + talachitasSmsResource)
        .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withHttpHeaders(HeaderNames.AUTHORIZATION -> authorization)
        .post(s""" {"message_type": $messageType, "phone_number": "$phoneNumber"}""")

      val g: WSResponse = Await.result(response, timeoutDatabaseSeconds)

      logger.info("body response sms: " + g.body)

    } catch {
      case e: Exception => logger.error("Error with sms ", e)
    } finally {
      logger.info("finished call sms ")
    }
  }
}
