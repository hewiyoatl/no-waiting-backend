package formatter

import models.{Address, AddressOutbound}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

object AddressFormatter {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val jodaDateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.toString())
  }

  val jodaDateReads = Reads[DateTime](js =>
    js.validate[String].map[DateTime](dtString =>
      DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  implicit val AddressReader: Reads[Address] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "address_1").read[String] and
      (JsPath \ "address_2").read[String] and
      (JsPath \ "zip_code").read[String] and
      (JsPath \ "suffix_zip_code").read[String] and
      (JsPath \ "state").read[String] and
      (JsPath \ "city").read[String] and
      (JsPath \ "country").read[String] and
      (JsPath \ "latitude").read[String] and
      (JsPath \ "longitude").read[String] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "deleted").read[Boolean]
    )(Address.apply _)

  implicit val AddressWriter: Writes[AddressOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "address_1").write[String] and
      (JsPath \ "address_2").write[String] and
      (JsPath \ "zip_code").write[String] and
      (JsPath \ "suffix_zip_code").write[String] and
      (JsPath \ "state").write[String] and
      (JsPath \ "city").write[String] and
      (JsPath \ "country").write[String] and
      (JsPath \ "latitude").write[String] and
      (JsPath \ "longitude").write[String] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(AddressOutbound.unapply))

}

