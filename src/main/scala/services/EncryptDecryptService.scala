package services

import java.io.File
import java.util.Base64

import com.google.crypto.tink._
import com.google.crypto.tink.aead.{AeadConfig, AeadKeyTemplates}
import com.google.crypto.tink.config.TinkConfig
import javax.inject.Inject
import play.api.Configuration

class EncryptDecryptService @Inject()(config: Configuration) {

  TinkConfig.register()

  AeadConfig.register()

  val keysetFilename = "keyset.json"

  val keyDirectory: String = config.get[String]("talachitas.env") match {
    case "local" => config.get[String]("talachitas.directory.cert.local")
    case "prod" => config.get[String]("talachitas.directory.cert.prod")
    case _ => throw new RuntimeException("Not found variable for 'talachitas.env' either local or prod for 'talachitas.directory.cert'")
  }

  val readKeyFile: KeysetHandle = {
    val keysetHandle: KeysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile (new File(keyDirectory + keysetFilename) ) )
    keysetHandle
  }

  val aead = readKeyFile.getPrimitive(classOf[Aead])

  def generateKeyFile(): Unit = {

    // Generate the key material...
    val keysetHandle:KeysetHandle  = KeysetHandle.generateNew(
      AeadKeyTemplates.AES256_GCM)

    CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(new File(keyDirectory + keysetFilename)))

  }

  def encrypt(plaintext: String): String = {
    val ciphertext = aead.encrypt(plaintext.getBytes, null)
    Base64.getEncoder.encodeToString(ciphertext)
  }

  def decrypt(encr: String): String = {
    val decryptBytes = aead.decrypt(Base64.getDecoder.decode(encr), null)
    new String(decryptBytes)
  }
}
