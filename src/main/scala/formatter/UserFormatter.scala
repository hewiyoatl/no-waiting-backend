package formatter

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class UserInbound(id: Option[Long],
                       mobile: String,
                       firstName: Option[String],
                       lastName: Option[String],
                       email: Option[String],
                       createdTimestamp: Option[DateTime])

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

  val UserReader: Reads[UserInbound] = (
    (JsPath \ "user_id").readNullable[Long] and
      (JsPath \ "mobile").read[String] and
      (JsPath \ "first_name").readNullable[String] and
      (JsPath \ "last_name").readNullable[String] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "created_timestamp").readNullable[DateTime](jodaDateReads)
    )(UserInbound.apply _)

  //  val htmlGenerateWriter: Writes[HtmlRequest] = (
  //    (JsPath \ "payload").write[JsValue] and
  //      (JsPath \ "answer_payload").write[JsValue]
  //    )(unlift(HtmlRequest.unapply))
  //
  //  val interviewOutboundWriter: Writes[InterviewOutboundCreation] = (
  //    (JsPath \ "template_uuid").write[String] and
  //      (JsPath \ "version").writeNullable[Long] and
  //      (JsPath \ "variation").writeNullable[Long]
  //    )(unlift(InterviewOutboundCreation.unapply))
  //
  //  val documentInboundCreationReader: Reads[DocumentInboundreation] = (
  //    (JsPath \ "template_uuid").read[String] and
  //      (JsPath \ "interview_uuid").readNullable[String] and
  //      (JsPath \ "document_name").readNullable[String] and
  //      (JsPath \ "html_rendered").readNullable[String]
  //    )(DocumentInboundCreation.apply _)
  //
  //  val documentOutboundCreationWriter: Writes[DocumentOutboundCreation] = (
  //    (JsPath \ "document_uuid").write[String] and
  //      (JsPath \ "document_name").writeNullable[String] and
  //      (JsPath \ "interview_uuid").write[String] and
  //      (JsPath \ "created_at").write[DateTime](jodaDateWrites) and
  //      (JsPath \ "updated_at").writeNullable[DateTime](jodaDateWrites) and
  //      (JsPath \ "template_uuid").writeNullable[String]
  //    )(unlift(DocumentOutboundCreation.unapply))
  //
  //  implicit val documentOutboundHistoryWriter: Writes[DocumentOutboundHistory] = (
  //    (JsPath \ "document_event_uuid").write[String] and
  //      (JsPath \ "document_event_type").write[Int] and
  //      (JsPath \ "event_name").write[String] and
  //      (JsPath \ "created_by_client").writeNullable[String] and
  //      (JsPath \ "created_at").write[DateTime](jodaDateWrites)
  //    )(unlift(DocumentOutboundHistory.unapply))
  //
  //  implicit val documentOutboundHistoryReader: Reads[DocumentOutboundHistory] = (
  //    (JsPath \ "document_event_uuid").read[String] and
  //      (JsPath \ "document_event_type").read[Int] and
  //      (JsPath \ "event_name").read[String] and
  //      (JsPath \ "created_by_client").readNullable[String] and
  //      (JsPath \ "created_at").read[DateTime](jodaDateReads)
  //    )(DocumentOutboundHistory.apply _)
  //
  //  //  implicit val documentHistoryList: Writes[DocumentOutboundHistoryList] =
  //  //    (__ \ "hist").writeNullable[DocumentOutboundHistory].contramap(_.historyList)
  //
  //  val documentOutboundGetWriter: Writes[DocumentOutboundGet] = (
  //    (JsPath \ "document_uuid").write[String] and
  //      (JsPath \ "document_name").write[String] and
  //      (JsPath \ "interview_uuid").write[String] and
  //      (JsPath \ "created_at").write[DateTime](jodaDateWrites) and
  //      (JsPath \ "updated_at").write[DateTime](jodaDateWrites) and
  //      (JsPath \ "html_rendered").writeNullable[String] and
  //      (JsPath \ "history").writeNullable[List[DocumentOutboundHistory]] and
  //      (JsPath \ "is_assigned").write[Boolean] and
  //      (JsPath \ "template_uuid").writeNullable[String]
  //    )(unlift(DocumentOutboundGet.unapply))
  //
  //  val documentOutboundGetReader: Reads[DocumentOutboundGet] = (
  //    (JsPath \ "document_uuid").read[String] and
  //      (JsPath \ "document_name").read[String] and
  //      (JsPath \ "interview_uuid").read[String] and
  //      (JsPath \ "created_at").read[DateTime](jodaDateReads) and
  //      (JsPath \ "updated_at").read[DateTime](jodaDateReads) and
  //      (JsPath \ "html_rendered").readNullable[String] and
  //      (JsPath \ "history").readNullable[List[DocumentOutboundHistory]] and
  //      (JsPath \ "is_assigned").read[Boolean] and
  //      (JsPath \ "template_uuid").readNullable[String]
  //    )(DocumentOutboundGet.apply _)
  //
  //  val documentInboundUpdateReader: Reads[DocumentInboundUpdate] = (
  //    (JsPath \ "document_name").readNullable[String] and
  //      (JsPath \ "html_rendered").readNullable[String]
  //    )(DocumentInboundUpdate.apply _)
  //
  //
  //  val documentInboundCopy = (__ \ "document_name").readNullable[String].map { l => DocumentInboundCopy(l) }
}

