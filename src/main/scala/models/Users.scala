package models

import com.google.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
                        bearerToken: Option[String])

class Users @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                      customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

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
        user.roles
      )).result.map(
      _.headOption.map {
        case (
          id,
          email,
          nickname,
          firstName,
          lastName,
          phoneNumber,
          roles) =>
          UserOutbound(
            id,
            Option(email),
            nickname,
            Option(firstName),
            Option(lastName),
            phoneNumber,
            Option(List(roles)),
            None
          )
      }
    ))

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
  }

}