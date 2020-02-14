package formatter

import models.ReservationOutbound
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ReservationInbound(id: Option[Long],
                              userId: Option[Long],
                              userType: Option[Long],
                              restaurantId: Option[Long],
                              status: Option[Int],
                              createdTimestamp: Option[DateTime])

object ReservationFormatter {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val jodaDateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.toString())
  }

  val jodaDateReads = Reads[DateTime](js =>
    js.validate[String].map[DateTime](dtString =>
      DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  val ReservationReader: Reads[ReservationInbound] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "user_id").readNullable[Long] and
      (JsPath \ "user_type").readNullable[Long] and
      (JsPath \ "restaurant_id").readNullable[Long] and
      (JsPath \ "status").readNullable[Int] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads)
    )(ReservationInbound.apply _)

  val ReservationWriter: Writes[ReservationOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "user_id").writeNullable[Long] and
      (JsPath \ "user_type").writeNullable[Long] and
      (JsPath \ "restaurant_id").writeNullable[Long] and
      (JsPath \ "status").writeNullable[Int] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(ReservationOutbound.unapply))
}

