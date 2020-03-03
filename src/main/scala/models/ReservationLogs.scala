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

case class ReservationLogsIn(id: Option[Long],
                             reservationId: Option[Long],
                             status: Option[String],
                             created_timestamp: Option[DateTime],
                             updated_timestamp: Option[DateTime])

case class ReservationLogsOutbound(id: Option[Long],
                                   reservationId: Option[Long],
                                   status: Option[String],
                                   createdTimestamp: Option[DateTime],
                                   updatedTimestamp: Option[DateTime])

class ReservationLogsTableDef(tag: Tag) extends Table[ReservationLogsIn](tag, Some("talachitas"), "reservation_logs") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def reservationId = column[Option[Long]]("reservation_id")

  def status = column[Option[String]]("status")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def updatedTimestamp = column[Option[DateTime]]("updated_timestamp")

  override def * =
    (id, reservationId, status, createdTimestamp, updatedTimestamp) <>(ReservationLogsIn.tupled, ReservationLogsIn.unapply)
}

class ReservationLogs @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val reservationLogs = TableQuery[ReservationLogsTableDef]

  def add(reservationLog: ReservationLogsIn): Future[Option[ReservationLogsOutbound]] = {
    db.run(
      ((reservationLogs returning reservationLogs.map(_.id)) += reservationLog).flatMap(newId => {

        reservationLogs.filter(reservationLog => reservationLog.id === newId).map(
          reservationLog => (
            reservationLog.id,
            reservationLog.reservationId,
            reservationLog.status,
            reservationLog.createdTimestamp,
            reservationLog.updatedTimestamp)).result.map(
            _.headOption.map {
              case (
                id,
                reservationId,
                status,
                createdTimestamp,
                updatedTimestamp
                ) =>
                ReservationLogsOutbound(
                  id,
                  reservationId,
                  status,
                  createdTimestamp,
                  updatedTimestamp
                )
            }
          )
      }).transactionally)
  }

//  def delete(id: Long): Future[Int] = {
//    db.run(reservationLogs.filter(_.id === id).map(u => u.deleted).update(Some(2)))
//  }

  def listAll(reservationId: Option[Long]): Future[Seq[ReservationLogsOutbound]] = {

    db.run(
      MaybeFilter(reservationLogs)
        .filter(reservationId)(v => d => d.reservationId === v).query.map(reservationLog =>
//        .filter(Some(2))(v => d => d.status =!= v).query.map(reservation =>
        (
          reservationLog.id,
          reservationLog.reservationId,
          reservationLog.status,
          reservationLog.createdTimestamp,
          reservationLog.updatedTimestamp)).result.map(
          _.seq.map {
            case (id, reservationId, status, createdTimestamp, updatedTimestamp) =>
              ReservationLogsOutbound(id, reservationId, status, createdTimestamp, updatedTimestamp)
          }
        )
    )
  }

  def retrieveReservationLog(id: Long): Future[Option[ReservationLogsOutbound]] = {

    db.run(reservationLogs.filter(reservationLog => reservationLog.id === id).map(
      reservationLog => (
        reservationLog.id,
        reservationLog.reservationId,
        reservationLog.status,
        reservationLog.createdTimestamp,
        reservationLog.updatedTimestamp)).result.map(
        _.headOption.map {
          case (id, reservationId, status, createdTimestamp, updatedTimestamp) =>
            ReservationLogsOutbound(id, reservationId, status, createdTimestamp, updatedTimestamp)
        }
      ))
  }

  def patchReservationLog(reservationLog: ReservationLogsIn): Future[Option[ReservationLogsOutbound]] = {

    db.run(
      reservationLogs.filter(r =>
        r.id === reservationLog.id).map(r =>
        (r.status)).update(
          reservationLog.status
        ).flatMap(x => {
        reservationLogs.filter(u => u.id === reservationLog.id).map(
          u => (u.id, u.reservationId, u.status, u.createdTimestamp, u.updatedTimestamp)).result.map(
            _.headOption.map {
              case (id, reservationId, status, createdTimestamp, updatedTimestamp) =>
                ReservationLogsOutbound(id, reservationId, status, createdTimestamp, updatedTimestamp)
            }
          )
      }).transactionally)
  }
}