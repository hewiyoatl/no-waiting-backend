package models

import com.google.inject.Inject
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RestUser(id: Option[Long],
                    sucursalId: Option[Long],
                    firstName: Option[String],
                    lastName: Option[String],
                    mobile: Option[String],
                    username: Option[String],
                    password: Option[String],
                    deleted: Boolean)

case class RestUserOutbound(id: Option[Long],
                            firstName: Option[String],
                            lastName: Option[String],
                            mobile: Option[String],
                            userName: Option[String])

case class RestUserFormData(firstName: String, lastName: String, mobile: String, email: String)

object RestUserForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "mobile" -> nonEmptyText,
      "email" -> email
    )(RestUserFormData.apply)(RestUserFormData.unapply)
  )
}

class RestUserTableDef(tag: Tag) extends Table[RestUser](tag, Some("talachitas"),"rest_user") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def sucursalId = column[Option[Long]]("sucursal_id")

  def firstName = column[Option[String]]("first_name")

  def lastName = column[Option[String]]("last_name")

  def mobile = column[Option[String]]("phone_number")

  def userName = column[Option[String]]("user_name")

  def password = column[Option[String]]("password")

  def deleted = column[Boolean]("deleted")

  override def * =
    (
      id,
      sucursalId,
      firstName,
      lastName,
      mobile,
      userName,
      password,
      deleted) <>(RestUser.tupled, RestUser.unapply)
}

class RestUsers @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                          customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val restUsers = TableQuery[RestUserTableDef]

  def add(restUser: RestUser): Future[Option[RestUserOutbound]] = {
    db.run(
      ((restUsers returning restUsers.map(_.id)) += restUser).flatMap(newId =>

        restUsers.filter(u => u.id === newId && u.deleted === false).map(
          u => (u.id, u.firstName, u.lastName, u.mobile, u.userName)).result.map(
            _.headOption.map {
              case (id, firstName, lastName, mobile, userName) =>
                RestUserOutbound(id, firstName, lastName, mobile, userName)
            }
          )).transactionally)

  }

  def delete(id: Long): Future[Int] = {
    db.run(restUsers.filter(_.id === id).map(u => u.deleted).update(true))
  }

  def listAll: Future[Seq[RestUserOutbound]] = {
    db.run(restUsers.filter(_.deleted === false).map(u =>
      (u.id, u.firstName, u.lastName, u.mobile, u.userName)).result.map(
        _.seq.map {
          case (id, firstName, lastName, mobile, userName) =>
            RestUserOutbound(id, firstName, lastName, mobile, userName)
        }
      )
    )
  }

  def retrieveRestUser(id: Long): Future[Option[RestUserOutbound]] = {
    db.run(restUsers.filter(u => u.id === id && u.deleted === false).map(
      u => (u.id, u.firstName, u.lastName, u.mobile, u.userName)).result.map(
        _.headOption.map {
          case (id, firstName, lastName, mobile, userName) =>
            RestUserOutbound(id, firstName, lastName, mobile, userName)
        }
      ))
  }

  def patchRestUser(restUser: RestUser): Future[Option[RestUserOutbound]] = {

    db.run(
      restUsers.filter(u =>
        u.id === restUser.id && u.deleted === false).map(u =>
        (u.firstName, u.lastName, u.mobile, u.password)).update(
          restUser.firstName, restUser.lastName, restUser.mobile, restUser.password
        ).flatMap(x => {

        restUsers.filter(u => u.id === restUser.id && u.deleted === false).map(
          u => (u.id, u.firstName, u.lastName, u.mobile, u.password)).result.map(
            _.headOption.map {
              case (id, firstName, lastName, mobile, password) =>
                RestUserOutbound(id, firstName, lastName, mobile, password)
            }
          )

      }).transactionally)

  }

}