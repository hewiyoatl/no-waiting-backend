package formatter

import models.{Address, AddressOutbound, Restaurant, RestaurantOutbound}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

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

  import AddressFormatter._

  val RestaurantReader: Reads[Restaurant] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "business_name").read[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "average_waiting_time").read[Long] and
      (JsPath \ "address_info").read[Address] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "updated_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "deleted").read[Boolean]
    )(Restaurant.apply _)

  val RestaurantWriter: Writes[RestaurantOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "business_name").write[String] and
      (JsPath \ "phone_number").writeNullable[String] and
      (JsPath \ "average_waiting_time").write[Long] and
      (JsPath \ "address_info").writeNullable[AddressOutbound] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites) and
      (JsPath \ "updated_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(RestaurantOutbound.unapply))
}

