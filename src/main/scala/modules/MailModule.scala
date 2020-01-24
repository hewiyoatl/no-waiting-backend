package modules

import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.mailer.{Email, MailerClient}

import scala.util.Try

case class SMTPConfigurationCustom(
                              host: String,
                              port: Int,
                              ssl: Boolean = false,
                              tls: Boolean = false,
                              tlsRequired: Boolean = false,
                              user: Option[String] = None,
                              password: Option[String] = None,
                              debugMode: Boolean = false,
                              timeout: Option[Int] = None,
                              connectionTimeout: Option[Int] = None,
                              props: Config = ConfigFactory.empty(),
                              mock: Boolean = false)

object SMTPConfigurationCustom {

  @inline
  private def getOptionString(config: Config, name: String) = {
    Try(config.getString(name)).toOption
  }

  @inline
  private def getOptionInt(config: Config, name: String) = {
    Try(config.getInt(name)).toOption
  }

  def apply(config: Config) = new SMTPConfigurationCustom(
    resolveHost(config),
    config.getInt("port"),
    config.getBoolean("ssl"),
    config.getBoolean("tls"),
    config.getBoolean("tlsRequired"),
    getOptionString(config, "user"),
    getOptionString(config, "password"),
    config.getBoolean("debug"),
    getOptionInt(config, "timeout"),
    getOptionInt(config, "connectiontimeout"),
    config.getConfig("props"),
    config.getBoolean("mock")
  )

  def resolveHost(config: Config): String = {
    if (config.getBoolean("mock")) {
      // host won't be used anyway...
      ""
    } else {
      getOptionString(config, "host").getOrElse(throw new RuntimeException("host needs to be set in order to use this plugin (or set play.mailer.mock to true in application.conf)"))
    }
  }

}


import javax.inject.{Inject, Provider}
import play.api.inject.Module
import play.api.{Configuration, Environment}

/**
  * Mail Module to provide emails
  */
class CustomMailerConfigurationModule extends Module {
  def bindings(environment: Environment, conf: Configuration) = Seq(
    bind[SMTPConfigurationCustom].toProvider[CustomSMTPConfigurationProvider]
  )
}

class CustomSMTPConfigurationProvider @Inject()(config: Configuration) extends Provider[SMTPConfigurationCustom] {
  override def get() = {

    // https://myaccount.google.com/u/1/security?pageId=none
    //Less secure app access needs to be on
    new SMTPConfigurationCustom(
      host = config.get[String]("play.mailer.host"),
      port = config.get[Int]("play.mailer.port"),
      ssl = config.get[Boolean]("play.mailer.ssl"),
      tls = config.get[Boolean]("play.mailer.tsl"),
      user = Some(config.get[String]("play.mailer.user")),
      password = Some(config.get[String]("play.mailer.password")),
      debugMode = config.get[Boolean]("play.mailer.debug")
    )
  }
}


class MailerService @Inject() (mailerClient: MailerClient) {

  def sendEmailHtml(subject: String, from: String, to: Seq[String], bodyHtml: String) = {

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
}