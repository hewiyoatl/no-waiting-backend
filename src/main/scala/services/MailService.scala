package services

import javax.inject.Inject
import org.apache.commons.mail.{HtmlEmail, MultiPartEmail}
import play.api.Configuration
import play.api.libs.mailer._

class MailerClientTalachitas @Inject() (config: Configuration,
                                        encryptDecryptService: EncryptDecryptService)
  extends MailerClient {

  private lazy val instance = {

      val overrideSmtpConfiguration = new SMTPConfiguration(
        host = encryptDecryptService.decrypt(config.get[String]("play.mailer.host")),
        port = config.get[Int]("play.mailer.port"),
        ssl = config.get[Boolean]("play.mailer.ssl"),
        tls = config.get[Boolean]("play.mailer.tls"),
        user = Some(encryptDecryptService.decrypt(config.get[String]("play.mailer.user"))),
        password = Some(encryptDecryptService.decrypt(config.get[String]("play.mailer.password"))),
        debugMode = config.get[Boolean]("play.mailer.debug"))

      new CommonsMailer(overrideSmtpConfiguration) {

        override def send(email: MultiPartEmail): String = email.send()

        override def createMultiPartEmail(): MultiPartEmail = new MultiPartEmail()

        override def createHtmlEmail(): HtmlEmail = new HtmlEmail()
      }
  }

  override def send(data: Email): String = {
    instance.send(data)
  }
}

class MailerService @Inject() (config: Configuration, mailerClient: MailerClientTalachitas) {

  final val resourcesDirectory: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.home.conf.local")
    case "prod" => config.get[String]("talachitas.home.conf.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.home.conf'")
  }

  private final val verifyEmailTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/emailVerification.html").getLines().mkString

  final val talachitasDomainBackend: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.home.backend.app.protocol.local") + config.get[String]("talachitas.home.backend.app.local")
    case "prod" => config.get[String]("talachitas.home.backend.app.protocol.prod") + config.get[String]("talachitas.home.backend.app.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.home.backend.app.protocal'")
  }

  private def sendEmailHtml(subject: String, from: String, to: Seq[String], bodyHtml: String) = {

    //how to send Seq[("", "")]
    val (x: String, y: String) = ("Content-type", "text/html")
    val z = (x, y)

    val email = Email(
      subject,
      from,
      to,
      None,
      Some(bodyHtml),
      None, //Some("UTF-8"),
      Seq.empty,
      Seq.empty,
      Seq.empty,
      None,
      Seq.empty,
      Seq.empty//Seq(z)
    )
    mailerClient.send(email)
  }

  def sendVerifyEmail(subject: String, sender: String, to: String, admin: String, welcomeMessage: String, verifyLinkWithToken: String): String = {
    val replaceVariables = verifyEmailTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
      .replaceAll("verifyEmailLink.html", talachitasDomainBackend + verifyLinkWithToken)
    this.sendEmailHtml(subject, sender, Seq(to, admin),  replaceVariables)
  }
}