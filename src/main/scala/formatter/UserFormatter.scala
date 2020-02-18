package formatter

import models.{UserIn, UserOutbound}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

object UserFormatter {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val jodaDateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.toString())
  }

  val jodaDateReads = Reads[DateTime](js =>
    js.validate[String].map[DateTime](dtString =>
      DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  val UserReader: Reads[UserIn] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "email").read[String] and
      (JsPath \ "nickname").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "first_name").read[String] and
      (JsPath \ "last_name").read[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "roles").read[String] and
      (JsPath \ "verify_email").readNullable[Boolean] and
      (JsPath \ "verify_phone").readNullable[Boolean] and
      (JsPath \ "retry_email").readNullable[Int] and
      (JsPath \ "retry_phone").readNullable[Int] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "updated_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "deleted").readNullable[Boolean]
    )(UserIn.apply _)

  val UserWriter: Writes[UserOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "email").write[String] and
      (JsPath \ "nickname").writeNullable[String] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "lastName").write[String] and
      (JsPath \ "phoneNumber").writeNullable[String] and
      (JsPath \ "roles").write[String] and
      (JsPath \ "bearerToken").writeNullable[String] and
      (JsPath \ "verifyEmail").writeNullable[Boolean] and
      (JsPath \ "retryEmail").writeNullable[Int] and
      (JsPath \ "verifyPhone").writeNullable[Boolean] and
      (JsPath \ "retryPhone").writeNullable[Int] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites) and
      (JsPath \ "updated_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(UserOutbound.unapply))

}

