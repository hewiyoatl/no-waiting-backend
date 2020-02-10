package services

import javax.inject.Inject
import play.api.http.HeaderNames
import play.api.i18n.Lang
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

// Our custom action implementation
class LanguageAction @Inject()(bodyParser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilder[Request, AnyContent] {

  val DEFAULT_LANGUAGE_MESSAGES: String = "en"

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {

    val newRequest = new WrappedRequest[A](request) {
      override lazy val acceptLanguages: Seq[Lang] = Seq(Lang(extractLanguage(request)))
    }
    block(newRequest)
  }

  private def extractLanguage[A](request: Request[A]): String =
    request.headers.get(HeaderNames.CONTENT_LANGUAGE).getOrElse(DEFAULT_LANGUAGE_MESSAGES)

  override protected def executionContext: ExecutionContext = ec

  override def parser: BodyParser[AnyContent] = bodyParser
}
