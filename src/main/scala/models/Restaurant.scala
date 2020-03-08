package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import utilities.DateTimeMapper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class RestaurantModel(id: Option[Long],
                           businessName: String,
                           phoneNumber: Option[String],
                           averageWaitingTime: Long,
                           addressId: Long,
                           createdTimestamp: Option[DateTime],
                           updatedTimestamp: Option[DateTime],
                           deleted: Boolean)

case class Restaurant(id: Option[Long],
                      businessName: String,
                      phoneNumber: Option[String],
                      averageWaitingTime: Long,
                      addressInfo: Address,
                      createdTimestamp: Option[DateTime],
                      updatedTimestamp: Option[DateTime],
                      deleted: Boolean)

case class RestaurantOutbound(id: Option[Long],
                              businessName: String,
                              phoneNumber: Option[String],
                              averageWaitingTime: Long,
                              addressInfo: Option[AddressOutbound],
                              createdTimestamp: Option[DateTime],
                              updatedTimestamp: Option[DateTime])

class RestaurantTableDef(tag: Tag) extends Table[RestaurantModel](tag, Some("talachitas"),"restaurant") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def businessName = column[String]("business_name")

  def phoneNumber = column[Option[String]]("phone_number")

  //average waiting time per restaurant in seconds for now
  def averageWaitingTime = column[Long]("average_waiting_time")

  def addressId = column[Long]("address_id")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def updatedTimestamp = column[Option[DateTime]]("updated_timestamp")

  def deleted = column[Boolean]("deleted")

  override def * = (
    id,
    businessName,
    phoneNumber,
    averageWaitingTime,
    addressId,
    createdTimestamp,
    updatedTimestamp,
    deleted) <>(RestaurantModel.tupled, RestaurantModel.unapply)
}

class Restaurants @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                            customizedSlickConfig: CustomizedSlickConfig,
                            config: Configuration)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val addresses = TableQuery[AddressTableDef]

  val restaurants = TableQuery[RestaurantTableDef]

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  def add(restaurant: RestaurantModel): Future[Int] = {

    val f: Future[Int] = db.run(
      (restaurants += restaurant).transactionally)

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
    db.run(restaurants.filter(_.id === id).map(u => u.deleted).update(true).transactionally)
  }

  def listAll: Future[Seq[RestaurantOutbound]] = {
    val leftOuterJoin = for {
      (r, a) <- restaurants.filter(_.deleted === false) joinLeft addresses on (_.addressId === _.id)
    } yield (
      r, a
    )

    db.run(leftOuterJoin.result.map(
      _.seq.map {
        case (r, a) => {

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

          RestaurantOutbound(
            r.id,
            r.businessName,
            r.phoneNumber,
            r.averageWaitingTime,
            addressInfo,
            r.createdTimestamp,
            r.updatedTimestamp)
        }
      }
    ).transactionally
    )
  }

  def retrieveRestaurantSync(id: Long): Option[RestaurantOutbound] = {
    val future:Future[Option[RestaurantOutbound]] = retrieveRestaurant(id)
    val response: Option[RestaurantOutbound] = Await.result(future, timeoutDatabaseSeconds)
    response
  }

  def retrieveRestaurant(id: Long): Future[Option[RestaurantOutbound]] = {
    val leftOuterJoin = for {
      (r, a) <- restaurants.filter(_.id === id).filter(_.deleted === false) joinLeft addresses on (_.addressId === _.id)
    } yield (
      r, a
    )

    db.run(leftOuterJoin.result.map(
      _.headOption.map {
        case (r, a) => {

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

          RestaurantOutbound(
            r.id,
            r.businessName,
            r.phoneNumber,
            r.averageWaitingTime,
            addressInfo,
            r.createdTimestamp,
            r.updatedTimestamp)
        }
      }
    ).transactionally)
  }

  def patchRestaurant(restaurant: RestaurantModel): Future[Option[RestaurantOutbound]] = {

    db.run(
      restaurants.filter(r =>
        r.id === restaurant.id && r.deleted === false).map(rr =>
        (
          rr.businessName,
          rr.phoneNumber,
          rr.averageWaitingTime,
          rr.addressId,
          rr.updatedTimestamp
        )).update(
        restaurant.businessName,
        restaurant.phoneNumber,
        restaurant.averageWaitingTime,
        restaurant.addressId,
        restaurant.updatedTimestamp
      ).flatMap(x => {
        restaurants.filter(rrr => rrr.id === restaurant.id && rrr.deleted === false).map(
          rrrr => (
            rrrr.id,
            rrrr.businessName,
            rrrr.phoneNumber,
            rrrr.averageWaitingTime,
            rrrr.addressId,
            rrrr.createdTimestamp,
            rrrr.updatedTimestamp,
            rrrr.deleted)).result.map(
          _.headOption.map {
            case (
              id,
              businessName,
              phoneNumber,
              averageWaitingTime,
              addressId,
              createdTimestamp,
              updatedTimestamp,
              deleted) =>
              RestaurantOutbound(
                id,
                businessName,
                phoneNumber,
                averageWaitingTime,
                None,
                createdTimestamp,
                updatedTimestamp)
          }
        )
      }).transactionally)
  }
}