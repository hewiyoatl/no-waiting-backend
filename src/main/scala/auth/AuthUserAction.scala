package auth

import formatter.{ErrorMessage, ErrorMessageFormatter}
import javax.inject.Inject
import play.api.http.HeaderNames
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc._
import utilities.Util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// A custom request type to hold our JWT claims, we can pass these on to the
// handling action
//case class UserRequest[A](jwt: JwtClaim, token: String, request: Request[A]) extends WrappedRequest[A](request)

// Our custom action implementation
class AuthUserAction @Inject()(bodyParser: BodyParsers.Default, authService: AuthService, util: Util)(implicit ec: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent] {

  implicit val errorWriter = ErrorMessageFormatter.errorWriter

  // A regex for parsing the Authorization header value
  private val headerTokenRegex = """Bearer (.+?)""".r

  override def parser: BodyParser[AnyContent] = bodyParser

  // Called when a request is invoked. We should validate the bearer token here
  // and allow the request to proceed if it is valid.
  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] =
    extractBearerToken(request) map { token =>
      authService.validateUserJwt(token) match {
        case Success(claim) => block(UserRequest(claim, token, request)) // token was valid - proceed!
        case Failure(t) => Future.successful(Results.Unauthorized(Json.toJson(ErrorMessage(UNAUTHORIZED, t.getMessage))).withHeaders(util.headersCors: _*)) // token was invalid - return 401
      }
    } getOrElse Future.successful(Results.Unauthorized(Json.toJson(ErrorMessage(UNAUTHORIZED, "Token not received"))).withHeaders(util.headersCors: _*)) // no token was sent - return 401

  // Helper for extracting the token value
  private def extractBearerToken[A](request: Request[A]): Option[String] =
    request.headers.get(HeaderNames.AUTHORIZATION) collect {
      case headerTokenRegex(token) => token
    }

  override protected def executionContext: ExecutionContext = ec
}
