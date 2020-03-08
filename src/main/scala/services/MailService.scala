package services

import javax.inject.Inject
import org.apache.commons.mail.{HtmlEmail, MultiPartEmail}
import play.api.{Configuration, Logger}
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

class MailerService @Inject() (config: Configuration,
                               mailerClient: MailerClientTalachitas,
                               encryptDecryptService: EncryptDecryptService,
                               redirectService: RedirectService) {

  final val resourcesDirectory: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.home.conf.local")
    case "prod" => config.get[String]("talachitas.home.conf.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.home.conf'")
  }
  private final val adminEmail: String = encryptDecryptService.decrypt(config.get[String]("play.mailer.admin"))

  private final val senderEmail: String = encryptDecryptService.decrypt(config.get[String]("play.mailer.user"))

  private final val verifyEmailTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/emailVerification.html").getLines().mkString

  private final val resetAccountTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/resetAccount.html").getLines().mkString

  private final val notifyInQueueTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/notifyInQueue.html").getLines().mkString

  private final val notifyAvailableTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/notifyAvailable.html").getLines().mkString

  private final val notifyCancelledTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/notifyCancelled.html").getLines().mkString

  private final val notifyCompletedTemplate : String = scala.io.Source.fromFile(resourcesDirectory + "templatesDirectory" + "/notifyCompleted.html").getLines().mkString

  val logger: Logger = Logger(this.getClass())

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

  def sendVerifyEmail(subject: String, to: String, welcomeMessage: String, verifyLinkWithToken: String): String = {
    val replaceVariables = verifyEmailTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
      .replaceAll("verifyEmailLink.html", redirectService.ENGLISH_DOMAIN_APP + "verifyEmail.html?emailVerification=" + verifyLinkWithToken)
    this.sendEmailHtml(subject, senderEmail, Seq(to, adminEmail),  replaceVariables)
  }

  def sendResetAccount(subject: String, to: String, welcomeMessage: String, resetAccountWithToken: String): String = {
    val replaceVariables = resetAccountTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
      .replaceAll("resetAccountLink.html", redirectService.ENGLISH_DOMAIN_APP + "resetAccountLink.html?resetAccountVerification=" + resetAccountWithToken)
    this.sendEmailHtml(subject, senderEmail, Seq(to, adminEmail),  replaceVariables)
  }

  def sendEmailNotifyInQueue(subject: String, to: String, welcomeMessage: String): String = {
    val replaceVariables = notifyInQueueTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
//      .replaceAll("resetAccountLink.html", redirectService.ENGLISH_DOMAIN_APP + "resetAccountLink.html")
    this.sendEmailHtml(subject, senderEmail, Seq(to, adminEmail),  replaceVariables)
  }

  def sendEmailNotifyAvailable(subject: String, to: String, welcomeMessage: String, googleMapsUrl: String): String = {
//    val googleURLEncode: String = URLEncoder.encode(googleMapsUrl, StandardCharsets.UTF_8.toString)
    val replaceVariables = notifyAvailableTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
      .replaceAll("<googleMaps />", s"""<a href="$googleMapsUrl">Enjoy your meal!</a>""")
//    logger.debug("Email for notify available 1 " + replaceVariables)
//    val replaceAgain = replaceVariables.replaceAll("<googleMaps />", s"""<a href="$googleMapsUrl">Enjoy your meal!</a>""")
    logger.debug("Email Replace all for notify available " + replaceVariables)
    this.sendEmailHtml(subject, senderEmail, Seq(to, adminEmail),  replaceVariables)
  }

  def sendEmailNotifyCancelled(subject: String, to: String, welcomeMessage: String): String = {
    val replaceVariables = notifyCancelledTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
    //      .replaceAll("resetAccountLink.html", redirectService.ENGLISH_DOMAIN_APP + "resetAccountLink.html")
    this.sendEmailHtml(subject, senderEmail, Seq(to, adminEmail),  replaceVariables)
  }

  def sendEmailNotifyCompleted(subject: String, to: String, welcomeMessage: String): String = {
    val replaceVariables = notifyCompletedTemplate
      .replaceAll("<welcomeMessage />", welcomeMessage)
    //      .replaceAll("resetAccountLink.html", redirectService.ENGLISH_DOMAIN_APP + "resetAccountLink.html")
    this.sendEmailHtml(subject, senderEmail, Seq(to, adminEmail),  replaceVariables)
  }

}