package formatter

import play.api.libs.functional.syntax._
import play.api.libs.json.{Writes, JsPath}

case class ErrorMessage(code: Int, message: String)

object ErrorMessageFormatter {

  val errorWriter: Writes[ErrorMessage] = (
    (JsPath \ "code").write[Int] and
      (JsPath \ "message").write[String]
    )(unlift(ErrorMessage.unapply))
}
