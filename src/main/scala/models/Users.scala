package models

import com.google.inject.Inject
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class UserIn(id: Option[Long],
                  email: String,
                  nickname: Option[String],
                  password: String,
                  firstName: String,
                  lastName: String,
                  phoneNumber: Option[String],
                  roles: String)

case class UserOutbound(id: Option[Long],
                        email: Option[String],
                        nickname: Option[String],
                        firstName: Option[String],
                        lastName: Option[String],
                        phoneNumber: Option[String],
                        roles: Option[List[String]],
                        bearerToken: Option[String],
                        verifyEmail: Option[Boolean],
                        verifyEmailRetry: Option[Int],
                        verifyPhone: Option[Boolean],
                        verifyPhoneRetry: Option[Int])

class Users @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                      config: Configuration,
                      customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val users = TableQuery[UsersTableDef]

//  def listContacts: Future[Seq[User]] = {
//    db.run(users.map(user =>
//      (user.email, user.nickname, user.password)).result.map(
//      _.seq.map {
//        case (email, nickname, password) =>
//          User(email, nickname, password)
//      }
//    ))
//  }

  def updateVerifyEmail(email: String, verifyEmail: Option[Boolean], retryEmail: Option[Int]): Int = {

    val future: Future[Int] = db.run(users.filter(u => u.email === email).map(user => (user.verifyEmail, user.retryEmail)).update(verifyEmail, retryEmail))

    val g = Await.result(future, timeoutDatabaseSeconds)

    g
  }

  def updateVerifyEmailAndPassword(password: String, email: String, verifyEmail: Option[Boolean], retryEmail: Option[Int]): Int = {

    val future: Future[Int] = db.run(users.filter(u => u.email === email).map(user => (user.password, user.verifyEmail, user.retryEmail)).update(password, verifyEmail, retryEmail))

    val g = Await.result(future, timeoutDatabaseSeconds)

    g
  }

  def addUser(user: UserIn): Future[Option[UserOutbound]] = {

    db.run(
      (users += user).transactionally)

    Future(Option(UserOutbound(
      None,
      Option(user.email),
      user.nickname,
      Option(user.firstName),
      Option(user.lastName),
      user.phoneNumber,
      Option(List(user.roles)),
      None,
      None,
      None,
      None,
      None)))
  }


  def retrieveUser(email: String, password: String): Future[Option[UserOutbound]] = {

    db.run(users.filter(u => u.email === email && u.password === password).map(user =>
      (
        user.id,
        user.email,
        user.nickname,
        user.firstName,
        user.lastName,
        user.phoneNumber,
        user.roles,
        user.verifyEmail,
        user.retryEmail,
        user.verifyPhone,
        user.retryPhone
      )).result.map(
      _.headOption.map {
        case (
          id,
          email,
          nickname,
          firstName,
          lastName,
          phoneNumber,
          roles,
          verifyEmail,
          retryEmail,
          verifyPhone,
          retryPhone) =>
          UserOutbound(
            id,
            Option(email),
            nickname,
            Option(firstName),
            Option(lastName),
            phoneNumber,
            Option(List(roles)),
            None,
            verifyEmail,
            retryEmail,
            verifyPhone,
            retryPhone)
      }
    ))
  }

  def retrieveUser(email: String): Future[Option[UserOutbound]] = {

    db.run(users.filter(u => u.email === email).map(user =>
      (
        user.id,
        user.email,
        user.nickname,
        user.firstName,
        user.lastName,
        user.phoneNumber,
        user.roles,
        user.verifyEmail,
        user.retryEmail,
        user.verifyPhone,
        user.retryPhone
      )).result.map(
      _.headOption.map {
        case (
          id,
          email,
          nickname,
          firstName,
          lastName,
          phoneNumber,
          roles,
          verifyEmail,
          retryEmail,
          verifyPhone,
          retryPhone) =>
          UserOutbound(
            id,
            Option(email),
            nickname,
            Option(firstName),
            Option(lastName),
            phoneNumber,
            Option(List(roles)),
            None,
            verifyEmail,
            retryEmail,
            verifyPhone,
            retryPhone)
      }
    ))
  }

  def verifyUser(email: String): Future[Boolean] = {

    db.run(users.filter(u => u.email === email).exists.result)
  }

  def verifyUserVerification(email: String): Future[Boolean] = {

    db.run(users.filter(u => u.email === email && u.verifyEmail.getOrElse(false) == true).exists.result)
  }

  //  def deleteContact(email: String): Future[Int] = {
//
//    db.run(users.filter(_.email === email).delete)
//  }

  class UsersTableDef(tag: Tag) extends Table[UserIn](tag, Some("talachitas"), "users") {

    override def * = (id, email, nickname, password, firstName, lastName, phoneNumber, roles) <> (UserIn.tupled, UserIn.unapply)

    def id = column[Option[Long]]("id", O.PrimaryKey)

    def email = column[String]("email")

    def nickname = column[Option[String]]("nickname")

    def password = column[String]("password")

    def roles = column[String]("roles")

    def firstName = column[String]("f_name")

    def lastName = column[String]("l_name")

    def phoneNumber = column[Option[String]]("phone")

    def verifyEmail = column[Option[Boolean]]("verify_email")

    def verifyPhone = column[Option[Boolean]]("verify_phone")

    def retryEmail = column[Option[Int]]("retry_email")

    def retryPhone = column[Option[Int]]("retry_phone")

  }
}