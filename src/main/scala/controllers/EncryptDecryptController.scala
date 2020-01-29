package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.{Configuration, Logger}
import services.{EncryptDecryptService, MetricsService}
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}

class EncryptDecryptController @Inject()(cc: ControllerComponents)
                                        (implicit context: ExecutionContext,
                                         metricsService: MetricsService,
                                         config: Configuration,
                                         encryptDecryptService: EncryptDecryptService,
                                         util: Util) extends AbstractController(cc) {

  val logger: Logger = Logger(this.getClass())

  def generateKeyFileAPI = Action.async { implicit request =>

    encryptDecryptService.generateKeyFile()

    Future(Ok("Key generated"))
  }

  def encryptDecrypt(plaintext: String) = Action.async { implicit request =>

    val encr = encryptDecryptService.encrypt(plaintext)

    val decr = encryptDecryptService.decrypt(encr)

    Future(Ok("Result " + encr + " " + decr))

  }

  def encrypt(plaintext: String) = Action.async { implicit request =>

    val encr = encryptDecryptService.encrypt(plaintext)

    Future(Ok("Result encryption " + encr))
  }

  def decrypt(encr: String) = Action.async { implicit request =>

    val decr = encryptDecryptService.decrypt(encr)

    Future(Ok("Result decryption " + decr))

  }
}
