package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import utilities.DateTimeMapper._
import utilities.MaybeFilter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Reservation(id: Option[Long],
                       userId: Option[Long],
                       userType: Option[Long],
                       locationId: Option[Long],
                       status: Option[Int],
                       created_timestamp: Option[DateTime])

case class ReservationOutbound(id: Option[Long],
                               userId: Option[Long],
                               userType: Option[Long],
                               locationId: Option[Long],
                               status: Option[Int],
                               createdTimestamp: Option[DateTime])

case class ReservationFormData(firstName: String, lastName: String, mobile: String, email: String)

class ReservationTableDef(tag: Tag) extends Table[Reservation](tag, Some("talachitas"), "reservation") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[Option[Long]]("user_id")

  def userType = column[Option[Long]]("user_type")

  def restaurantId = column[Option[Long]]("restaurant_id")

  def status = column[Option[Int]]("status")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  override def * =
    (id, userId, userType, restaurantId, status, createdTimestamp) <>(Reservation.tupled, Reservation.unapply)
}

class Reservations @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                             customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val reservations = TableQuery[ReservationTableDef]

  def add(reservation: Reservation): Future[Option[ReservationOutbound]] = {
    db.run(
      ((reservations returning reservations.map(_.id)) += reservation).flatMap(newId => {

        reservations.filter(reservation => reservation.id === newId).map(
          reservation => (
            reservation.id,
            reservation.userId,
            reservation.userType,
            reservation.restaurantId,
            reservation.status,
            reservation.createdTimestamp)).result.map(
            _.headOption.map {
              case (
                id,
                userId,
                userType,
                locationId,
                status,
                createdTimestamp
                ) =>
                ReservationOutbound(
                  id,
                  userId,
                  userType,
                  locationId,
                  status,
                  createdTimestamp
                )
            }
          )

      }).transactionally)
  }

  def delete(id: Long): Future[Int] = {
    // TODO: WE NEED TO SET THE STATUS CODES. HERE 2 MEANS CANCCELLED
    db.run(reservations.filter(_.id === id).map(u => u.status).update(Some(2)))
  }

  def listAll(location: Option[Long]): Future[Seq[ReservationOutbound]] = {

    // TODO: WE NEED TO SET THE STATUS CODES. HERE 2 MEANS THAT IT IS CANCELLED.
    db.run(
      MaybeFilter(reservations)
        .filter(location)(v => d => d.restaurantId === v)
        .filter(Some(2))(v => d => d.status =!= v).query.map(reservation =>
        (
          reservation.id,
          reservation.userId,
          reservation.userType,
          reservation.restaurantId,
          reservation.status,
          reservation.createdTimestamp)).result.map(
          _.seq.map {
            case (id, userId, userType, locationId, status, createdTimestamp) =>
              ReservationOutbound(id, userId, userType, locationId, status, createdTimestamp)
          }
        )
    )
  }

  def retrieveReservation(id: Long): Future[Option[ReservationOutbound]] = {

    // TODO: WE NEED TO SET STATUS CODES
    db.run(reservations.filter(reservation => reservation.id === id && reservation.status =!= Option(2)).map(
      reservation => (
        reservation.id,
        reservation.userId,
        reservation.userType,
        reservation.restaurantId,
        reservation.status,
        reservation.createdTimestamp)).result.map(
        _.headOption.map {
          case (
            id, userId, userType, locationId, status, createdTimestamp) =>
            ReservationOutbound(id, userId, userType, locationId, status, createdTimestamp)
        }
      ))
  }

  def patchReservation(reservation: Reservation): Future[Option[ReservationOutbound]] = {

    db.run(
      reservations.filter(r =>
        r.id === reservation.id && r.status =!= Option(2)).map(r =>
        (r.status)).update(
          reservation.status
        ).flatMap(x => {

        // TODO: SET THE CODE
        reservations.filter(u => u.id === reservation.id && u.status =!= Option(2)).map(
          u => (u.id, u.userId, u.userType, u.restaurantId, u.status, u.createdTimestamp)).result.map(
            _.headOption.map {
              case (id, userId, userType, locationId, status, createdTimestamp) =>
                ReservationOutbound(id, userId, userType, locationId, status, createdTimestamp)
            }
          )

      }).transactionally)
  }

}