package formatter

import models.RestUserOutbound
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class RestUserInbound(id: Option[Long],
                           userId: Option[Long],
                           restaurantId: Option[Long],
                           createdTimestamp: Option[DateTime])

object RestUserFormatter {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val jodaDateWrites: Writes[DateTime] = new Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.toString())
  }

  val jodaDateReads = Reads[DateTime](js =>
    js.validate[String].map[DateTime](dtString =>
      DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
    )
  )

  val RestUserReader: Reads[RestUserInbound] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "user_id").readNullable[Long] and
      (JsPath \ "restaurant_id").readNullable[Long] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads)
    )(RestUserInbound.apply _)

  val RestUserWriter: Writes[RestUserOutbound] = (
    (JsPath \ "id").writeNullable[Long] and
      (JsPath \ "user_id").writeNullable[Long] and
      (JsPath \ "restaurant_id").writeNullable[Long]
    )(unlift(RestUserOutbound.unapply))
}

