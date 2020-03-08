package models

//import java.sql.Date

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// valid status are: enum('STARTED','IN-QUEUE','AVAILABLE','COMPLETED','CANCELLED')
object ReservationStatus extends Enumeration {
  val STARTED = "STARTED"
  val IN_QUEUE = "IN_QUEUE"
  val AVAILABLE = "AVAILABLE"
  val COMPLETED = "COMPLETED"
  val CANCELLED = "CANCELLED"
}
case class ReservationModel(id: Option[Long],
                       userId: Long,
                       restaurantId: Long,
                       status: String,
                       comments: Option[String],
                       sourceAddressId: Long,
                       destinationAddressId: Long,
                       waitingTimeCreation: Long,
                       waitingTimeCounting: Long,
                       createdTimestamp: Option[DateTime],
                       updatedTimestamp: Option[DateTime],
                       deleted: Boolean)

case class Reservation(id: Option[Long],
                            userId: Long,
                            restaurantId: Long,
                            status: String,
                            comments: Option[String],
                            sourceAddressId: Option[Address],
                            destinationAddressId: Option[Address],
                            waitingTimeCreation: Option[Long],
                            waitingTimeCounting: Option[Long],
                            createdTimestamp: Option[DateTime],
                            updatedTimestamp: Option[DateTime],
                            deleted: Option[Boolean])

case class ReservationOutbound(id: Option[Long],
                               userId: Long,
                               userName: String,
                               restaurantId: Long,
                               restaurantName: String,
                               status: String,
                               comments: Option[String],
                               sourceAddress: Option[AddressOutbound],
                               destinationAddress: Option[AddressOutbound],
                               waitingTimeCreation: Long,
                               waitingTimeCounting: Long,
                               deleted: Boolean,
                               createdTimestamp: Option[DateTime],
                               updatedTimestamp: Option[DateTime])

case class ReservationOutboundWithLogs(reservation: ReservationOutbound,
                                       reservationLogs: Option[Seq[ReservationLogsOutbound]])

//needed to parse dates for slick
import utilities.DateTimeMapper._

class ReservationTableDef(tag: Tag) extends Table[ReservationModel](tag, Some("talachitas"), "reservation") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[Long]("user_id")

  def restaurantId = column[Long]("restaurant_id")

  def status = column[String]("status")

  def comments = column[Option[String]]("comments")

  def sourceAddressId = column[Long]("source_address_id")

  def destinationAddressId = column[Long]("destination_address_id")

  def waitingTimeCreation = column[Long]("waiting_time_creation")

  def waitingTimeCounting = column[Long]("waiting_time_counting")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def updatedTimestamp = column[Option[DateTime]]("updated_timestamp")

  def deleted = column[Boolean]("deleted")

  override def * =
     (id,
      userId,
      restaurantId,
      status,
      comments,
      sourceAddressId,
      destinationAddressId,
      waitingTimeCreation,
      waitingTimeCounting,
      createdTimestamp,
      updatedTimestamp,
      deleted) <>(ReservationModel.tupled, ReservationModel.unapply)
}

class Reservations @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                             config: Configuration,
                             customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val addresses = TableQuery[AddressTableDef]

  val reservations = TableQuery[ReservationTableDef]

  val restaurants = TableQuery[RestaurantTableDef]

  val users = TableQuery[UsersTableDef]

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  def add(reservation: ReservationModel): Future[Option[Long]] = {

    val q1 = (reservations returning reservations.map(_.id)) += reservation

    val future: Future[Option[Long]] = db.run { q1.transactionally }

    future

    //    Await.result(future, timeoutDatabaseSeconds)
  }

  def delete(id: Long): Future[Int] = {
    db.run(reservations.filter(_.id === id).map(u => u.deleted).update(true))
  }

  def listAll(status: String): Future[Seq[ReservationOutbound]] = {
    val monadicInnerJoin = for {
      r   <- reservations if (r.deleted === false && r.status === status)
      rest <- restaurants if (r.restaurantId === rest.id)
      addressRestaurant <- addresses if (addressRestaurant.id === rest.addressId)
      u   <- users if (u.id === r.userId)
      addressUser <- addresses if (addressUser.id === u.addressId)
    } yield (r, rest, addressRestaurant, u, addressUser)

    db.run(monadicInnerJoin.result.map(
      _.seq.map {
        case (r, rest, addressRestaurant, u, addressUser) => {

          val addressInfoUser: AddressOutbound =
            AddressOutbound(
              addressUser.id,
              addressUser.address1,
              addressUser.address2,
              addressUser.zipCode,
              addressUser.suffixZipCode,
              addressUser.state,
              addressUser.city,
              addressUser.country,
              addressUser.latitude,
              addressUser.longitude,
              addressUser.createdTimestamp)

          val addressInfoRestaurant: AddressOutbound =
            AddressOutbound(
              addressRestaurant.id,
              addressRestaurant.address1,
              addressRestaurant.address2,
              addressRestaurant.zipCode,
              addressRestaurant.suffixZipCode,
              addressRestaurant.state,
              addressRestaurant.city,
              addressRestaurant.country,
              addressRestaurant.latitude,
              addressRestaurant.longitude,
              addressRestaurant.createdTimestamp)

          ReservationOutbound(
            r.id,
            u.id.getOrElse(0),
            u.firstName + " " + u.lastName,
            rest.id.getOrElse(0),
            rest.businessName,
            r.status,
            r.comments,
            Some(addressInfoUser),
            Some(addressInfoRestaurant),
            r.waitingTimeCreation,
            r.waitingTimeCounting,
            r.deleted,
            r.createdTimestamp,
            r.updatedTimestamp)
        }
      }

    ))
  }

  def listArchiveReservation: Future[Seq[ReservationOutbound]] = {

    val monadicInnerJoin = for {
      r   <- reservations if (r.deleted === false && r.status === ReservationStatus.COMPLETED || r.status === ReservationStatus.CANCELLED)
      rest <- restaurants if (r.restaurantId === rest.id)
      addressRestaurant <- addresses if (addressRestaurant.id === rest.addressId)
      u   <- users if (u.id === r.userId)
      addressUser <- addresses if (addressUser.id === u.addressId)
    } yield (r, rest, addressRestaurant, u, addressUser)

    db.run(monadicInnerJoin.result.map(
      _.seq.map {
        case (r, rest, addressRestaurant, u, addressUser) => {

          val addressInfoUser: AddressOutbound =
            AddressOutbound(
              addressUser.id,
              addressUser.address1,
              addressUser.address2,
              addressUser.zipCode,
              addressUser.suffixZipCode,
              addressUser.state,
              addressUser.city,
              addressUser.country,
              addressUser.latitude,
              addressUser.longitude,
              addressUser.createdTimestamp)

          val addressInfoRestaurant: AddressOutbound =
            AddressOutbound(
              addressRestaurant.id,
              addressRestaurant.address1,
              addressRestaurant.address2,
              addressRestaurant.zipCode,
              addressRestaurant.suffixZipCode,
              addressRestaurant.state,
              addressRestaurant.city,
              addressRestaurant.country,
              addressRestaurant.latitude,
              addressRestaurant.longitude,
              addressRestaurant.createdTimestamp)

          ReservationOutbound(
            r.id,
            u.id.getOrElse(0),
            u.firstName + " " + u.lastName,
            rest.id.getOrElse(0),
            rest.businessName,
            r.status,
            r.comments,
            Some(addressInfoUser),
            Some(addressInfoRestaurant),
            r.waitingTimeCreation,
            r.waitingTimeCounting,
            r.deleted,
            r.createdTimestamp,
            r.updatedTimestamp)
        }
      }

    ))
  }

  def listActiveReservation: Future[Seq[ReservationOutbound]] = {

    val monadicInnerJoin = for {
//      r   <- reservations if (r.deleted === false && r.status =!= ReservationStatus.CANCELLED)
      r   <- reservations if (r.deleted === false && (r.status === ReservationStatus.STARTED || r.status === ReservationStatus.IN_QUEUE || r.status === ReservationStatus.AVAILABLE))
      rest <- restaurants if (r.restaurantId === rest.id)
      addressRestaurant <- addresses if (addressRestaurant.id === rest.addressId)
      u   <- users if (u.id === r.userId)
      addressUser <- addresses if (addressUser.id === u.addressId)
    } yield (r, rest, addressRestaurant, u, addressUser)

    db.run(monadicInnerJoin.result.map(
      _.seq.map {
        case (r, rest, addressRestaurant, u, addressUser) => {

          val addressInfoUser: AddressOutbound =
            AddressOutbound(
                addressUser.id,
                addressUser.address1,
                addressUser.address2,
                addressUser.zipCode,
                addressUser.suffixZipCode,
                addressUser.state,
                addressUser.city,
                addressUser.country,
                addressUser.latitude,
                addressUser.longitude,
                addressUser.createdTimestamp)

          val addressInfoRestaurant: AddressOutbound =
              AddressOutbound(
                addressRestaurant.id,
                addressRestaurant.address1,
                addressRestaurant.address2,
                addressRestaurant.zipCode,
                addressRestaurant.suffixZipCode,
                addressRestaurant.state,
                addressRestaurant.city,
                addressRestaurant.country,
                addressRestaurant.latitude,
                addressRestaurant.longitude,
                addressRestaurant.createdTimestamp)

          ReservationOutbound(
            r.id,
            u.id.getOrElse(0),
            u.firstName + " " + u.lastName,
            rest.id.getOrElse(0),
            rest.businessName,
            r.status,
            r.comments,
            Some(addressInfoUser),
            Some(addressInfoRestaurant),
            r.waitingTimeCreation,
            r.waitingTimeCounting,
            r.deleted,
            r.createdTimestamp,
            r.updatedTimestamp)
        }
      }

    ))
  }

  def reservationsToProcess: Future[Seq[ReservationOutbound]] = {

    val monadicInnerJoin = for {
      r   <- reservations if ((r.status === "STARTED" || r.status === "IN_QUEUE" || r.status === "AVAILABLE") && r.deleted === false)
    } yield (r)

    db.run(monadicInnerJoin.result.map(
      _.seq.map {
        case (r) =>
          ReservationOutbound(
            r.id,
            r.userId,
            "",
            0,
            "",
            r.status,
            r.comments,
            None,
            None,
            r.waitingTimeCreation,
            r.waitingTimeCounting,
            r.deleted,
            r.createdTimestamp,
            r.updatedTimestamp)
      }
    )
    )
  }

  def retrieveReservation(id: Long): Future[Seq[ReservationOutbound]] = {

    val monadicInnerJoin = for {
      r   <- reservations if (r.deleted === false && r.id === id)
      rest <- restaurants if (r.restaurantId === rest.id)
      addressRestaurant <- addresses if (addressRestaurant.id === rest.addressId)
      u   <- users if (u.id === r.userId)
      addressUser <- addresses if (addressUser.id === u.addressId)
    } yield (r, rest, addressRestaurant, u, addressUser)

    db.run(monadicInnerJoin.result.map(
      _.seq.map {
        case (r, rest, addressRestaurant, u, addressUser) => {

          val addressInfoUser: AddressOutbound =
            AddressOutbound(
              addressUser.id,
              addressUser.address1,
              addressUser.address2,
              addressUser.zipCode,
              addressUser.suffixZipCode,
              addressUser.state,
              addressUser.city,
              addressUser.country,
              addressUser.latitude,
              addressUser.longitude,
              addressUser.createdTimestamp)

          val addressInfoRestaurant: AddressOutbound =
            AddressOutbound(
              addressRestaurant.id,
              addressRestaurant.address1,
              addressRestaurant.address2,
              addressRestaurant.zipCode,
              addressRestaurant.suffixZipCode,
              addressRestaurant.state,
              addressRestaurant.city,
              addressRestaurant.country,
              addressRestaurant.latitude,
              addressRestaurant.longitude,
              addressRestaurant.createdTimestamp)

          ReservationOutbound(
            r.id,
            u.id.getOrElse(0),
            u.firstName + " " + u.lastName,
            rest.id.getOrElse(0),
            rest.businessName,
            r.status,
            r.comments,
            Some(addressInfoUser),
            Some(addressInfoRestaurant),
            r.waitingTimeCreation,
            r.waitingTimeCounting,
            r.deleted,
            r.createdTimestamp,
            r.updatedTimestamp)
        }
      }

    ))
  }

  def patchReservation(reservation: ReservationModel): Future[Option[ReservationOutbound]] = {

    db.run(
      reservations.filter(r => r.id === reservation.id && r.deleted === false).map(r =>
        (r.status, r.comments, r.updatedTimestamp)).update(reservation.status, reservation.comments, reservation.updatedTimestamp).flatMap(x => {
        reservations.filter(u => u.id === reservation.id).map(
          u => (u.id, u.userId, u.restaurantId, u.status, u.comments, u.sourceAddressId, u.destinationAddressId, u.waitingTimeCreation, u.waitingTimeCounting, u.createdTimestamp, u.updatedTimestamp, u.deleted)).result.map(
            _.headOption.map {
              case (id, userId, restaurantId, status, comments, sourceAddressId, destinationAddressId, waitingTimeCreation, waitingTimeCounting, createdTimestamp, updatedTimestamp, deleted) =>
                ReservationOutbound(id, userId, "", restaurantId, "", status, comments, None, None, waitingTimeCreation, waitingTimeCounting, deleted, createdTimestamp, updatedTimestamp)
            }
          )
      }).transactionally)
  }

  def updateWaitingTime(reservationId: Long, waitingTimeCounting: Long): Int = {

    val future: Future[Int] = db.run(reservations.filter(u => u.id === reservationId && u.deleted === false)
      .map(r => (r.waitingTimeCounting)).update(waitingTimeCounting))

    val g = Await.result(future, timeoutDatabaseSeconds)

    g

  }
}