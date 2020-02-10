package formatter

import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, JsPath}

case class Success(code: Int, message: String, data: Option[String])

object SuccessFormatter {

  val successWriter: Writes[Success] = (
    (JsPath \ "code").write[Int] and
      (JsPath \ "message").write[String] and
      (JsPath \ "data").writeNullable[String]
    )(unlift(Success.unapply))

}
