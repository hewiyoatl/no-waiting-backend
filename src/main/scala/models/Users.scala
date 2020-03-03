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

case class UserIn(     id: Option[Long],
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
                       addressInfo: Option[Address],
                       createdTimestamp: Option[DateTime],
                       updatedTimestamp: Option[DateTime],
                       deleted: Option[Boolean])

case class UserModelIn(id: Option[Long],
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
                       addressId: Option[Long],
                       createdTimestamp: Option[DateTime],
                       updatedTimestamp: Option[DateTime],
                       deleted: Option[Boolean])

//case class UserModelOutbound(id: Option[Long],
//                        email: String,
//                        nickname: Option[String],
//                        firstName: String,
//                        lastName: String,
//                        phoneNumber: Option[String],
//                        roles: String,
//                        bearerToken: Option[String],
//                        verifyEmail: Option[Boolean],
//                        retryEmail: Option[Int],
//                        verifyPhone: Option[Boolean],
//                        retryPhone: Option[Int],
//                        addressId: Option[Long],
//                        createdTimestamp: Option[DateTime],
//                        updatedTimestamp: Option[DateTime])

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
                             addressInfo: Option[AddressOutbound],
                             createdTimestamp: Option[DateTime],
                             updatedTimestamp: Option[DateTime])

class UsersTableDef(tag: Tag) extends Table[UserModelIn](tag, Some("talachitas"), "users") {

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

  def addressId = column[Option[Long]]("address_id")

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
    addressId,
    createdTimestamp,
    updatedTimestamp,
    deleted
  ) <> (UserModelIn.tupled, UserModelIn.unapply)
}

class Users @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                      config: Configuration,
                      customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val users = TableQuery[UsersTableDef]

  val addresses = TableQuery[AddressTableDef]

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

  def addUser(user: UserModelIn): Future[Int] = {

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

    val leftOuterJoin = for {
      (u, a) <- users.filter(_.deleted === false) joinLeft addresses on (_.addressId === _.id)
    } yield (
      u, a
    )

    db.run(leftOuterJoin.result.map(
      _.seq.map {
        case (u, a) => {
          val addressInfo: Option[AddressOutbound] =
            if (a.map(_.id).getOrElse(0) == 0) {
              None
            } else {
              Some(AddressOutbound(
                a.map(_.id).flatten,
                a.map(_.address1).get,
                a.map(_.address2).get,
                a.map(_.zipCode).get,
                a.map(_.suffixZipCode).get,
                a.map(_.state).get,
                a.map(_.city).get,
                a.map(_.country).get,
                a.map(_.latitude).get,
                a.map(_.longitude).get,
                a.map(_.createdTimestamp).get))
            }

          UserOutbound(
            u.id,
            u.email,
            u.nickname,
            u.firstName,
            u.lastName,
            u.phoneNumber,
            u.roles,
            None,
            u.verifyEmail,
            u.retryEmail,
            u.verifyPhone,
            u.retryPhone,
            addressInfo,
            u.createdTimestamp,
            u.updatedTimestamp)

        }
      }
    )
    )
  }

  def retrieveUserSync(id: Long): Option[UserOutbound] = {
    val future: Future[Option[UserOutbound]] = retrieveUser(id)
    val response: Option[UserOutbound] = Await.result(future, timeoutDatabaseSeconds)
    response
  }

  def retrieveUser(id: Long): Future[Option[UserOutbound]] = {
    val leftOuterJoin = for {
      (u, a) <- users.filter(_.id === id).filter(_.deleted === false) joinLeft addresses on (_.addressId === _.id)
    } yield (
      u, a
    )

    db.run(leftOuterJoin.result.map(
      _.headOption.map {
        case (u, a) => {

          val addressInfo: Option[AddressOutbound] =
            if (a.map(_.id).getOrElse(0) == 0) {
              None
            } else {
              Some(AddressOutbound(
                a.map(_.id).flatten,
                a.map(_.address1).get,
                a.map(_.address2).get,
                a.map(_.zipCode).get,
                a.map(_.suffixZipCode).get,
                a.map(_.state).get,
                a.map(_.city).get,
                a.map(_.country).get,
                a.map(_.latitude).get,
                a.map(_.longitude).get,
                a.map(_.createdTimestamp).get))
            }

          UserOutbound(
            u.id,
            u.email,
            u.nickname,
            u.firstName,
            u.lastName,
            u.phoneNumber,
            u.roles,
            None,
            u.verifyEmail,
            u.retryEmail,
            u.verifyPhone,
            u.retryPhone,
            addressInfo,
            u.createdTimestamp,
            u.updatedTimestamp)
        }
      }
    ))
  }

  def patchUser(user: UserModelIn): Future[Option[UserOutbound]] = {

    db.run(
      users.filter(u =>
        u.id === user.id && u.deleted === false).map(u =>
        (
          u.nickname,
          u.roles,
          u.firstName,
          u.lastName,
          u.phoneNumber,
          u.addressId,
          u.updatedTimestamp
        )).update(
        user.nickname,
        user.roles,
        user.firstName,
        user.lastName,
        user.phoneNumber,
        user.addressId,
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
            u.addressId,
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
              addressId,
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
                None,
                createdTimestamp,
                updatedTimestamp)
          }
        )
      }).transactionally)
  }

  def retrieveUser(email: String, password: String): Future[Option[UserOutbound]] = {

    val leftOuterJoin = for {
      (u, a) <- users.filter(_.email === email).filter(_.password === password).filter(_.deleted === false) joinLeft addresses on (_.addressId === _.id)
    } yield (
      u, a
    )

    db.run(leftOuterJoin.result.map(
      _.headOption.map {
        case (u, a) => {

          val addressInfo: Option[AddressOutbound] =
            if (a.map(_.id).getOrElse(0) == 0) {
              None
            } else {
              Some(AddressOutbound(
                a.map(_.id).flatten,
                a.map(_.address1).get,
                a.map(_.address2).get,
                a.map(_.zipCode).get,
                a.map(_.suffixZipCode).get,
                a.map(_.state).get,
                a.map(_.city).get,
                a.map(_.country).get,
                a.map(_.latitude).get,
                a.map(_.longitude).get,
                a.map(_.createdTimestamp).get))
            }

          UserOutbound(
            u.id,
            u.email,
            u.nickname,
            u.firstName,
            u.lastName,
            u.phoneNumber,
            u.roles,
            None,
            u.verifyEmail,
            u.retryEmail,
            u.verifyPhone,
            u.retryPhone,
            addressInfo,
            u.createdTimestamp,
            u.updatedTimestamp)
        }
      }
    ))
  }

  def retrieveUser(email: String): Future[Option[UserOutbound]] = {

    val leftOuterJoin = for {
      (u, a) <- users.filter(_.email === email).filter(_.deleted === false) joinLeft addresses on (_.addressId === _.id)
    } yield (
      u, a
    )

    db.run(leftOuterJoin.result.map(
      _.headOption.map {
        case (u, a) => {

          val addressInfo: Option[AddressOutbound] =
            if (a.map(_.id).getOrElse(0) == 0) {
              None
            } else {
              Some(AddressOutbound(
                a.map(_.id).flatten,
                a.map(_.address1).get,
                a.map(_.address2).get,
                a.map(_.zipCode).get,
                a.map(_.suffixZipCode).get,
                a.map(_.state).get,
                a.map(_.city).get,
                a.map(_.country).get,
                a.map(_.latitude).get,
                a.map(_.longitude).get,
                a.map(_.createdTimestamp).get))
            }

          UserOutbound(
            u.id,
            u.email,
            u.nickname,
            u.firstName,
            u.lastName,
            u.phoneNumber,
            u.roles,
            None,
            u.verifyEmail,
            u.retryEmail,
            u.verifyPhone,
            u.retryPhone,
            addressInfo,
            u.createdTimestamp,
            u.updatedTimestamp)
        }
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