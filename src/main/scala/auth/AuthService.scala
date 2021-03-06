package auth

import java.security.spec.{ECParameterSpec, ECPoint, ECPrivateKeySpec, ECPublicKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import javax.inject.Inject
import models.{UserIn, UserModelIn, UserOutbound}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.joda.time.DateTime
import pdi.jwt._
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsObject, JsString, Json}
import services.EncryptDecryptService

import scala.util.{Failure, Success, Try}

class AuthService @Inject()(config: Configuration, encryptDecryptService: EncryptDecryptService) {

  val logger: Logger = Logger(this.getClass())

  val publicKey: PublicKey = {

    val X = BigInt(x, 16)
    val Y = BigInt(y, 16)
    val curveParams = ECNamedCurveTable.getParameterSpec("P-521")
    val curveSpec: ECParameterSpec = new ECNamedCurveSpec(
      "P-521",
      curveParams.getCurve(),
      curveParams.getG(),
      curveParams.getN(),
      curveParams.getH())

    val publicSpec = new ECPublicKeySpec(new ECPoint(X.underlying(), Y.underlying()), curveSpec)

    import java.security.Security
    Security.addProvider(new BouncyCastleProvider)
    val publicKeyEC = KeyFactory.getInstance("ECDSA", "BC").generatePublic(publicSpec)

    publicKeyEC

  }

  val privateKey: PrivateKey = {

    val S = BigInt(s, 16)
    val curveParams = ECNamedCurveTable.getParameterSpec("P-521")
    val curveSpec: ECParameterSpec = new ECNamedCurveSpec(
      "P-521",
      curveParams.getCurve(),
      curveParams.getG(),
      curveParams.getN(),
      curveParams.getH())

    val privateSpec = new ECPrivateKeySpec(S.underlying(), curveSpec)
    import java.security.Security
    Security.addProvider(new BouncyCastleProvider)
    val privateKeyEC = KeyFactory.getInstance("ECDSA", "BC").generatePrivate(privateSpec)

    privateKeyEC

  }

  // A regex that defines the JWT pattern and allows us to
  // extract the header, claims and signature
  private val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r

  // Splits a JWT into it's 3 component parts
  private val splitToken = (jwt: String) => jwt match {
    case jwtRegex(header, body, sig) => Success((header, body, sig))
    case _ => Failure(new Exception("Token does not match the correct pattern"))
  }

  // .. add the new methods below 'validateJwt'
  // As the header and claims data are base64-encoded, this function
  // decodes those elements
  private val decodeElements = (data: Try[(String, String, String)]) => data map {

    case (header, body, sig) =>
      (JwtBase64.decodeString(header), JwtBase64.decodeString(body), sig)
  }

  // Gets the JWK from the JWKS endpoint using the jwks-rsa library
  //  private val getJwk = (token: String) =>
  //    (splitToken andThen decodeElements) (token) flatMap {
  //      case (header, _, _) =>
  //        val jwtHeader = JwtJson.parseHeader(header) // extract the header
  //      val jwkProvider = new UrlJwkProvider(s"https://$domain")
  //
  //        // Use jwkProvider to load the JWKS data and return the JWK
  //        jwtHeader.keyId.map { k =>
  //          Try(jwkProvider.get(k))
  //        } getOrElse Failure(new Exception("Unable to retrieve kid"))
  //    }
  private val validateAdminClaims = (claims: JwtClaim) => {

    val roles: Option[String] = (Json.parse(claims.content) \ "roles").asOpt[String]
    val isAdmin: Boolean = roles.map(_.contains("Admin")).getOrElse(false)

    if (claims.expiration.get > System.currentTimeMillis) {
      if (isAdmin) {
        Success(claims)
      } else {
        Failure(new Exception("Unable to grant this operation, check your privileges"))
      }
    } else {
      Failure(new Exception("Token expired, please verify"))
    }
  }

  private val validateUserClaims = (claims: JwtClaim) => {

    val roles: Option[String] = (Json.parse(claims.content) \ "roles").asOpt[String]

    val isUser: Boolean = roles.map(_.contains("User")).getOrElse(false)

    val isClient: Boolean = roles.map(_.contains("Client")).getOrElse(false)

    val isAdmin: Boolean = roles.map(_.contains("Admin")).getOrElse(false)

    if (claims.expiration.get > System.currentTimeMillis) {
      if (isUser || isClient || isAdmin) {
        Success(claims)
      } else {
        Failure(new Exception("Unable to grant this operation for this, check your privileges"))
      }
    } else {
      Failure(new Exception("Token expired"))
    }
  }

  private val validateEmailsClaims = (claims: JwtClaim) => {

//    val roles: Option[String] = (Json.parse(claims.content) \ "roles").asOpt[String]
//
//    val isUser: Boolean = roles.map(_.contains("User")).getOrElse(false)

    if (claims.expiration.get > System.currentTimeMillis) {
      Success(claims)
    } else {
      Failure(new Exception("Token expired"))
    }
  }

  private val validateResetAccountClaims = (claims: JwtClaim) => {

    val reset: Option[String] = (Json.parse(claims.content) \ "reset").asOpt[String]
    val email: Option[String] = (Json.parse(claims.content) \ "email").asOpt[String]

    //
    //    val isUser: Boolean = roles.map(_.contains("User")).getOrElse(false)

    if (claims.expiration.get > System.currentTimeMillis || reset.getOrElse("no") == "yes" || email.getOrElse("") != "") {
      Success(claims)
    } else {
      Failure(new Exception("Token expired or this is not a reset token"))
    }
  }

  // Validates a JWT and potentially returns the claims if the token was
  // successfully parsed and validated
  def validateUserJwt(token: String): Try[JwtClaim] = for {

    //    jwk <- getJwk(token) // Get the secret key for this token

    claims <- JwtJson.decode(token, publicKey, Seq(JwtAlgorithm.ES512)) // Decode the token using the secret key

    _ <- validateUserClaims(claims) // validate the data stored inside the token
  } yield claims

  def validateEmailJwt(token: String): Try[JwtClaim] = for {

    //    jwk <- getJwk(token) // Get the secret key for this token

    claims <- JwtJson.decode(token, publicKey, Seq(JwtAlgorithm.ES512)) // Decode the token using the secret key

    _ <- validateEmailsClaims(claims) // validate the data stored inside the token
  } yield claims

  def validateResetAccountJwt(token: String): Try[JwtClaim] = for {

    //    jwk <- getJwk(token) // Get the secret key for this token

    claims <- JwtJson.decode(token, publicKey, Seq(JwtAlgorithm.ES512)) // Decode the token using the secret key

    _ <- validateResetAccountClaims(claims) // validate the data stored inside the token
  } yield claims

  def validateAdminJwt(token: String): Try[JwtClaim] = for {

    //    jwk <- getJwk(token) // Get the secret key for this token

    claims <- JwtJson.decode(token, publicKey, Seq(JwtAlgorithm.ES512)) // Decode the token using the secret key

    _ <- validateAdminClaims(claims) // validate the data stored inside the token
  } yield claims

  private def x : String = encryptDecryptService.decrypt(config.get[String]("auth.x"))
  private def y = encryptDecryptService.decrypt(config.get[String]("auth.y"))
  private def s = encryptDecryptService.decrypt(config.get[String]("auth.s"))

//  private def x : String = config.get[String]("auth.x")
//  private def y = config.get[String]("auth.y")
//  private def s = config.get[String]("auth.s")

  private def expiration = config.get[Int]("auth.expiration")
  private def expirationEmailVerification = config.get[Int]("auth.expirationVerifyEmails")
  private def expirationResetAccount      = config.get[Int]("auth.expirationResetAccount")

  // Your Auth0 audience, read from configuration
  //  private def audience = config.get[String]("auth0.audience")

  // The issuer of the token. For Auth0, this is just your Auth0
  // domain including the URI scheme and a trailing slash.
  //  private def issuer = s"https://$domain/"

  // Your Auth0 domain, read from configuration
  //  private def domain = config.get[String]("auth0.domain")

  def provideTokenLogin(user: UserOutbound): JsObject = {

    val message =      s"""{"email":"${user.email}",
                          |"first_name":"${user.firstName}",
                          |"last_name":"${user.lastName}",
                          |"roles": "${user.roles}",
                          |"exp": ${(new DateTime()).plusSeconds(expiration).getMillis},
                          |"iat": ${System.currentTimeMillis()}}""".stripMargin

    logger.info("Message to encode " + message)

    val token = Jwt.encode(message,
      privateKey,
      JwtAlgorithm.ES512)

    Json.obj(
      "email" -> JsString(user.email),
      "first_name" -> JsString(user.firstName),
      "last_name" -> JsString(user.lastName),
      "roles" -> Json.toJson(user.roles.map(x => x)),
      "phone_number" -> JsString(user.phoneNumber.getOrElse("")),
      "user_id" -> JsString(String.valueOf(user.id.getOrElse(0))),
      "nickname" -> user.nickname.map(JsString(_)),
      "bearer_token" -> token)

  }

  def provideTokenEmailVerification(userIn: UserModelIn, language: String): String = {

    val message =    s""" {"email":"${userIn.email}",
                          |"first_name":"${userIn.firstName}",
                          |"last_name":"${userIn.lastName}",
                          |"language":"${language}",
                          |"roles": "${userIn.roles}",
                          |"exp": ${(new DateTime()).plusDays(expirationEmailVerification).getMillis},
                          |"iat": ${System.currentTimeMillis()}}""".stripMargin

    val token = Jwt.encode(message,
      privateKey,
      JwtAlgorithm.ES512)

    token
  }

  def provideTokenResetAccount(userIn: UserModelIn, language: String): String = {

    val message =    s""" {"email":"${userIn.email}",
                          |"first_name":"${userIn.firstName}",
                          |"last_name":"${userIn.lastName}",
                          |"reset": "yes",
                          |"language":"${language}",
                          |"roles": "${userIn.roles}",
                          |"exp": ${(new DateTime()).plusDays(expirationResetAccount).getMillis},
                          |"iat": ${System.currentTimeMillis()}}""".stripMargin

    val token = Jwt.encode(message,
      privateKey,
      JwtAlgorithm.ES512)

    token
  }

  /**
    * value can be the password per see + a salt
    *
    * https://stackoverflow.com/questions/6840206/sha2-password-hashing-in-java
    *
    * @param value
    * @return
    */
  def getSha256(value: String) : String = {
    org.apache.commons.codec.digest.DigestUtils.sha256Hex(value)
  }

  def decodeBasicAuth(authHeader: String): (String, String) = {
    val baStr = authHeader.replaceFirst("Basic ", "")
    val decoded = new sun.misc.BASE64Decoder().decodeBuffer(baStr)
    val Array(user, password) = new String(decoded).split(":")
    (user, password)
  }
}