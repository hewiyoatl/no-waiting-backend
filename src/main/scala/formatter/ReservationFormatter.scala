package formatter

import models._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

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

  import AddressFormatter._

  val ReservationReader: Reads[Reservation] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "user_id").read[Long] and
      (JsPath \ "restaurant_id").read[Long] and
      (JsPath \ "status").read[String] and
      (JsPath \ "comments").readNullable[String] and
      (JsPath \ "source_address_info").readNullable[Address] and
      (JsPath \ "destination_address_info").readNullable[Address] and
      (JsPath \ "waiting_time_creation").readNullable[Long] and
      (JsPath \ "waiting_time_counting").readNullable[Long] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "updated_timestamp").readNullable[DateTime](jodaDateReads) and
      (JsPath \ "deleted").readNullable[Boolean]
    )(Reservation.apply _)

  implicit val ReservationWriter: Writes[ReservationOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "user_id").write[Long] and
      (JsPath \ "user_name").write[String] and
      (JsPath \ "restaurant_id").write[Long] and
      (JsPath \ "restaurant_name").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "comments").writeNullable[String] and
      (JsPath \ "source_address_info").writeNullable[AddressOutbound] and
      (JsPath \ "destination_address_info").writeNullable[AddressOutbound] and
      (JsPath \ "waiting_time_creation").write[Long] and
      (JsPath \ "waiting_time_counting").write[Long] and
      (JsPath \ "deleted").write[Boolean] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites) and
      (JsPath \ "updated_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(ReservationOutbound.unapply))

  implicit val ReservationLogWriter: Writes[ReservationLogsOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "reservation_id").writeNullable[Long] and
      (JsPath \ "status").writeNullable[String] and
      (JsPath \ "comments").writeNullable[String] and
      (JsPath \ "created_timestamp").writeNullable[DateTime](jodaDateWrites) and
      (JsPath \ "updated_timestamp").writeNullable[DateTime](jodaDateWrites)
    )(unlift(ReservationLogsOutbound.unapply))

  val ReservationWriterWithLogs: Writes[ReservationOutboundWithLogs] = (
    (JsPath \ "reservation").write[ReservationOutbound] and
      (JsPath \ "reservation_logs").writeNullable[Seq[ReservationLogsOutbound]]
    )(unlift(ReservationOutboundWithLogs.unapply))
}

