package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

//needed to parse dates for slick
import utilities.DateTimeMapper._

case class UserIn(id: Option[Long],
                  email: String,
                  nickname: Option[String],
                  password: Option[String],
                  firstName: String,
                  lastName: String,
                  phoneNumber: Option[String],
                  roles: String,
                  verifyEmail: Option[Boolean],
                  verifyPhone: Option[Boolean],
                  retryEmail: Option[Int],
                  retryPhone: Option[Int],
                  createdTimestamp: Option[DateTime],
                  updatedTimestamp: Option[DateTime],
                  deleted: Option[Boolean])

case class UserOutbound(id: Option[Long],
                        email: String,
                        nickname: Option[String],
                        firstName: String,
                        lastName: String,
                        phoneNumber: Option[String],
                        roles: String,
                        bearerToken: Option[String],
                        verifyEmail: Option[Boolean],
                        retryEmail: Option[Int],
                        verifyPhone: Option[Boolean],
                        retryPhone: Option[Int],
                        createdTimestamp: Option[DateTime],
                        updatedTimestamp: Option[DateTime])

class UsersTableDef(tag: Tag) extends Table[UserIn](tag, Some("talachitas"), "users") {

  def id = column[Option[Long]]("id", O.PrimaryKey)

  def email = column[String]("email")

  def nickname = column[Option[String]]("nickname")

  def password = column[Option[String]]("password")

  def firstName = column[String]("f_name")

  def lastName = column[String]("l_name")

  def phoneNumber = column[Option[String]]("phone")

  def roles = column[String]("roles")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def updatedTimestamp = column[Option[DateTime]]("updated_timestamp")

  def deleted = column[Option[Boolean]]("deleted")

  def verifyEmail = column[Option[Boolean]]("verify_email")

  def verifyPhone = column[Option[Boolean]]("verify_phone")

  def retryEmail = column[Option[Int]]("retry_email")

  def retryPhone = column[Option[Int]]("retry_phone")

  override def * = (
    id,
    email,
    nickname,
    password,
    firstName,
    lastName,
    phoneNumber,
    roles,
    verifyEmail,
    verifyPhone,
    retryEmail,
    retryPhone,
    createdTimestamp,
    updatedTimestamp,
    deleted
  ) <> (UserIn.tupled, UserIn.unapply)
}

class Users @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                      config: Configuration,
                      customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val users = TableQuery[UsersTableDef]

  def updateVerifyEmail(email: String, verifyEmail: Option[Boolean], retryEmail: Option[Int]): Int = {

    val future: Future[Int] = db.run(users.filter(u => u.email === email).map(user => (user.verifyEmail, user.retryEmail)).update(verifyEmail, retryEmail))

    val g = Await.result(future, timeoutDatabaseSeconds)

    g
  }

  def updateVerifyEmailAndPassword(password: Option[String], email: String, verifyEmail: Option[Boolean], retryEmail: Option[Int]): Int = {

    val future: Future[Int] = db.run(users.filter(u => u.email === email).map(user => (user.password, user.verifyEmail, user.retryEmail)).update(password, verifyEmail, retryEmail))

    val g = Await.result(future, timeoutDatabaseSeconds)

    g
  }

  def addUser(user: UserIn): Future[Int] = {

    val f: Future[Int] = db.run(
      (users += user).transactionally)

    f.onSuccess {
      case s =>
        logger.debug(s"Success Result: $s")
    }

    f.onComplete {
      case s =>
        logger.debug(s"Complete Result: $s")
    }

    f.onFailure {
      case s =>
        logger.debug(s"Complete Result: $s")
    }

    f
  }

  def delete(id: Long): Future[Int] = {
    db.run(users.filter(_.id === id).map(u => u.deleted).update(Some(true)))
  }

  def listAll: Future[Seq[UserOutbound]] = {

    db.run(users.filter(_.deleted === false).map(u =>
      (
        u.id,
        u.email,
        u.nickname,
        u.password,
        u.firstName,
        u.lastName,
        u.phoneNumber,
        u.roles,
        u.verifyEmail,
        u.verifyPhone,
        u.retryEmail,
        u.retryPhone,
        u.createdTimestamp,
        u.updatedTimestamp,
        u.deleted)).result.map(
      _.seq.map {
        case (
          id,
          email,
          nickname,
          password,
          firstName,
          lastName,
          phoneNumber,
          roles,
          verifyEmail,
          verifyPhone,
          retryEmail,
          retryPhone,
          createdTimestamp,
          updatedTimestamp,
          deleted) =>
          UserOutbound(
            id,
            email,
            nickname,
            firstName,
            lastName,
            phoneNumber,
            roles,
            None,
            verifyEmail,
            retryEmail,
            verifyPhone,
            retryPhone,
            createdTimestamp,
            updatedTimestamp)
      }
    )
    )
  }

  def retrieveUser(id: Long): Future[Option[UserOutbound]] = {
    db.run(users.filter(u => u.id === id && u.deleted === false).map(
      u => (
        u.id,
        u.email,
        u.nickname,
        u.password,
        u.firstName,
        u.lastName,
        u.phoneNumber,
        u.roles,
        u.verifyEmail,
        u.verifyPhone,
        u.retryEmail,
        u.retryPhone,
        u.createdTimestamp,
        u.updatedTimestamp,
        u.deleted)).result.map(
      _.headOption.map {
        case (
          id,
          email,
          nickname,
          password,
          firstName,
          lastName,
          phoneNumber,
          roles,
          verifyEmail,
          verifyPhone,
          retryEmail,
          retryPhone,
          createdTimestamp,
          updatedTimestamp,
          deleted) =>
          UserOutbound(
            id,
            email,
            nickname,
            firstName,
            lastName,
            phoneNumber,
            roles,
            None,
            verifyEmail,
            retryEmail,
            verifyPhone,
            retryPhone,
            createdTimestamp,
            updatedTimestamp)
      }
    ))
  }

  def patchUser(user: UserIn): Future[Option[UserOutbound]] = {

    db.run(
      users.filter(u =>
        u.id === user.id && u.deleted === false).map(u =>
        (
          u.nickname,
          u.roles,
          u.firstName,
          u.lastName,
          u.phoneNumber,
          u.updatedTimestamp
        )).update(
        user.nickname,
        user.roles,
        user.firstName,
        user.lastName,
        user.phoneNumber,
        user.updatedTimestamp
      ).flatMap(x => {

        users.filter(u => u.id === user.id && u.deleted === false).map(
          u => (
            u.id,
            u.email,
            u.nickname,
            u.password,
            u.firstName,
            u.lastName,
            u.phoneNumber,
            u.roles,
            u.verifyEmail,
            u.verifyPhone,
            u.retryEmail,
            u.retryPhone,
            u.createdTimestamp,
            u.updatedTimestamp,
            u.deleted)).result.map(
          _.headOption.map {
            case (
              id,
              email,
              nickname,
              password,
              firstName,
              lastName,
              phoneNumber,
              roles,
              verifyEmail,
              verifyPhone,
              retryEmail,
              retryPhone,
              createdTimestamp,
              updatedTimestamp,
              deleted) =>
              UserOutbound(
                id,
                email,
                nickname,
                firstName,
                lastName,
                phoneNumber,
                roles,
                None,
                verifyEmail,
                retryEmail,
                verifyPhone,
                retryPhone,
                createdTimestamp,
                updatedTimestamp)
          }
        )
      }).transactionally)
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
        user.retryPhone,
        user.createdTimestamp,
        user.updatedTimestamp
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
          retryPhone,
          createdTimestamp,
          updatedTimestamp) =>
          UserOutbound(
            id,
            email,
            nickname,
            firstName,
            lastName,
            phoneNumber,
            roles,
            None,
            verifyEmail,
            retryEmail,
            verifyPhone,
            retryPhone,
            createdTimestamp,
            updatedTimestamp)
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
        user.retryPhone,
        user.createdTimestamp,
        user.updatedTimestamp
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
          retryPhone,
          createdTimestamp,
          updatedTimestamp) =>
          UserOutbound(
            id,
            email,
            nickname,
            firstName,
            lastName,
            phoneNumber,
            roles,
            None,
            verifyEmail,
            retryEmail,
            verifyPhone,
            retryPhone,
            createdTimestamp,
            updatedTimestamp)
      }
    ))
  }

  def verifyUser(email: String): Future[Boolean] = {

    db.run(users.filter(u => u.email === email).exists.result)
  }

  def verifyUserVerification(email: String): Future[Boolean] = {

    db.run(users.filter(u => u.email === email && u.verifyEmail.getOrElse(false) == true).exists.result)
  }

}