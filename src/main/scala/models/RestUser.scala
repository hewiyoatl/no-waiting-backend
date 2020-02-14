package models

import com.google.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RestUser(id: Option[Long],
                    userId: Option[Long],
                    restaurantId: Option[Long],
                    deleted: Boolean)

case class RestUserOutbound(id: Option[Long],  userId: Option[Long], restaurantId: Option[Long])

class RestUserTableDef(tag: Tag) extends Table[RestUser](tag, Some("talachitas"),"rest_user") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[Option[Long]]("user_id")

  def restaurantId = column[Option[Long]]("restaurant_id")

  def deleted = column[Boolean]("deleted")

  override def * =
    (
      id,
      userId,
      restaurantId,
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
          u => (u.id, u.userId, u.restaurantId, u.deleted)).result.map(
            _.headOption.map {
              case (id, userId, restaurantId, deleted) =>
                RestUserOutbound(id, userId, restaurantId)
            }
          )).transactionally)

  }

  def delete(id: Long): Future[Int] = {
    db.run(restUsers.filter(_.id === id).map(u => u.deleted).update(true))
  }

  def listAll: Future[Seq[RestUserOutbound]] = {
    db.run(restUsers.filter(_.deleted === false).map(u =>
      (u.id, u.userId, u.restaurantId, u.deleted)).result.map(
        _.seq.map {
          case (id, userId, restaurantId, deleted) =>
            RestUserOutbound(id, userId, restaurantId)
        }
      )
    )
  }

  def retrieveRestUser(id: Long): Future[Option[RestUserOutbound]] = {
    db.run(restUsers.filter(u => u.id === id && u.deleted === false).map(
      u => (u.id, u.userId, u.restaurantId, u.deleted)).result.map(
        _.headOption.map {
          case (id, userId, restaurantId, deleted) =>
            RestUserOutbound(id, userId, restaurantId)
        }
      ))
  }

  def patchRestUser(restUser: RestUser): Future[Option[RestUserOutbound]] = {

    db.run(
      restUsers.filter(u =>
        u.id === restUser.id && u.deleted === false).map(u =>
        (u.deleted)).update(restUser.deleted
        ).flatMap(x => {

        restUsers.filter(u => u.id === restUser.id && u.deleted === false).map(
          u => (u.id, u.userId, u.restaurantId, u.deleted)).result.map(
            _.headOption.map {
              case (id, userId, restaurantId, deleted) =>
                RestUserOutbound(id, userId, restaurantId)
            }
          )

      }).transactionally)
  }
}