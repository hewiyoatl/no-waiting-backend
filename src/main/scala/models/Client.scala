package model

import com.google.inject.Inject
import models.{CustomizedSlickConfig, HasDatabaseConfigProviderTalachitas}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Client(id: Option[Long],
                  firstName: Option[String],
                  lastName: Option[String],
                  mobile: String,
                  email: Option[String],
                  deleted: Boolean)

case class ClientOutbound(id: Option[Long],
                          firstName: Option[String],
                          lastName: Option[String],
                          mobile: String,
                          email: Option[String])

case class ClientFormData(firstName: String, lastName: String, mobile: String, email: String)

object ClientForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "mobile" -> nonEmptyText,
      "email" -> email
    )(ClientFormData.apply)(ClientFormData.unapply)
  )
}

class ClientTableDef(tag: Tag) extends Table[Client](tag, Some("talachitas"), "client") {

  override def * =
    (id, firstName, lastName, mobile, email, deleted) <> (Client.tupled, Client.unapply)

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def firstName = column[Option[String]]("first_name")

  def lastName = column[Option[String]]("last_name")

  def mobile = column[String]("phone_number")

  def email = column[Option[String]]("email")

  def deleted = column[Boolean]("deleted")
}

class Clients @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                        customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val clients = TableQuery[ClientTableDef]

  def add(client: Client): Future[Option[ClientOutbound]] = {

    db.run(

      ((clients returning clients.map(_.id)) += client).flatMap(newId =>

        clients.filter(u => u.id === newId && u.deleted === false).map(u =>

          (u.id, u.firstName, u.lastName, u.mobile, u.email)).result.map(_.headOption.map {

          case (id, firstName, lastName, mobile, email) =>
            ClientOutbound(id, firstName, lastName, mobile, email)
        })

      ).transactionally)
  }

  def delete(id: Long): Future[Int] = {
    db.run(clients.filter(_.id === id).map(u => u.deleted).update(true))
  }

  def listAll: Future[Seq[ClientOutbound]] = {
    db.run(clients.filter(_.deleted === false).map(u =>
      (u.id, u.firstName, u.lastName, u.mobile, u.email)).result.map(
      _.seq.map {
        case (id, firstName, lastName, mobile, email) =>
          ClientOutbound(id, firstName, lastName, mobile, email)
      }
    )
    )
  }

  def retrieveClient(id: Long): Future[Option[ClientOutbound]] = {
    db.run(clients.filter(u => u.id === id && u.deleted === false).map(
      u => (u.id, u.firstName, u.lastName, u.mobile, u.email)).result.map(
      _.headOption.map {
        case (id, firstName, lastName, mobile, email) =>
          ClientOutbound(id, firstName, lastName, mobile, email)
      }
    ))
  }

  def patchClient(client: Client): Future[Option[ClientOutbound]] = {

    db.run(

      clients.filter(u => u.mobile === client.mobile && u.deleted === false).map(u => (u.firstName, u.lastName, u.email))
        .update(client.firstName, client.lastName, client.email)
        .flatMap(x =>

          clients.filter(u => u.mobile === client.mobile && u.deleted === false).map(u =>
            (u.id, u.firstName, u.lastName, u.mobile, u.email)).result.map(_.headOption.map {
            case (id, firstName, lastName, mobile, email) => ClientOutbound(id, firstName, lastName, mobile, email)

          })).transactionally)

  }

}