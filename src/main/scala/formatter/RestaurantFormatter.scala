package formatter

import models.RestaurantOutbound
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class RestaurantInbound(id: Option[Long],
                             businessName: String,
                             address1: String,
                             address2: String,
                             zipCode: String,
                             suffixZipCode: Option[String],
                             state: String,
                             city: String,
                             country: String,
                             phoneNumber: Option[String],
                             latitude: Float,
                             longitude: Float,
                             createdTimestamp: Option[DateTime],
                             updatedTimestamp: Option[DateTime])

object RestaurantFormatter {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val jodaDateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.toString())
  }

  val jodaDateReads = Reads[DateTime](js =>
    js.validate[String].map[DateTime](dtString =>
      DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  val RestaurantReader: Reads[RestaurantInbound] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "business_name").read[String] and
      (JsPath \ "address_1").read[String] and
      (JsPath \ "address_2").read[String] and
      (JsPath \ "zip_code").read[String] and
      (JsPath \ "suffix_zip_code").readNullable[String] and
      (JsPath \ "state").read[String] and
      (JsPath \ "city").read[String] and
      (JsPath \ "country").read[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "latitude").read[Float] and
      (JsPath \ "longitude").read[Float] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "updated_timestamp").readNullable[DateTime](jodaDateReads)
    )(RestaurantInbound.apply _)

  val RestaurantWriter: Writes[RestaurantOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "business_name").write[String] and
      (JsPath \ "address_1").write[String] and
      (JsPath \ "address_2").write[String] and
      (JsPath \ "zip_code").write[String] and
      (JsPath \ "suffix_zip_code").writeNullable[String] and
      (JsPath \ "state").write[String] and
      (JsPath \ "city").write[String] and
      (JsPath \ "country").write[String] and
      (JsPath \ "phone_number").writeNullable[String] and
      (JsPath \ "latitude").write[Float] and
      (JsPath \ "longitude").write[Float] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites) and
      (JsPath \ "updated_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(RestaurantOutbound.unapply))
}

