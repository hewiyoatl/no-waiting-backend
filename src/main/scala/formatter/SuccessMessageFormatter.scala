package formatter

import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, JsPath}

case class SuccessMessage(code: Int, message: String, data: Option[String])

object SuccessMessageFormatter {

  val successWriter: Writes[SuccessMessage] = (
    (JsPath \ "code").write[Int] and
      (JsPath \ "message").write[String] and
      (JsPath \ "data").writeNullable[String]
    )(unlift(SuccessMessage.unapply))

}
