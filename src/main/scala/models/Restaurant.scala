package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import utilities.DateTimeMapper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Restaurant(id: Option[Long],
                      businessName: String,
                      address1: String,
                      address2: String,
                      zipCode: String,
                      suffixZipCode: Option[String],
                      state: String,
                      city: String,
                      country: String,
                      phoneNumber: Option[String],
                      latitude: Float,
                      longitude: Float,
                      createdTimestamp: Option[DateTime],
                      updatedTimestamp: Option[DateTime],
                      deleted: Boolean)

case class RestaurantOutbound(id: Option[Long],
                              businessName: String,
                              address1: String,
                              address2: String,
                              zipCode: String,
                              suffixZipCode: Option[String],
                              state: String,
                              city: String,
                              country: String,
                              phoneNumber: Option[String],
                              latitude: Float,
                              longitude: Float,
                              createdTimestamp: Option[DateTime],
                              updatedTimestamp: Option[DateTime])

class RestaurantTableDef(tag: Tag) extends Table[Restaurant](tag, Some("talachitas"),"restaurant") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def businessName = column[String]("business_name")

  def address1 = column[String]("address_1")

  def address2 = column[String]("address_2")

  def zipCode = column[String]("zip_code")

  def suffixZipCode = column[Option[String]]("suffix_zip_code")

  def state = column[String]("state")

  def city = column[String]("city")

  def country = column[String]("country")

  def phoneNumber = column[Option[String]]("phone_number")

  def latitude = column[Float]("latitude")

  def longitude = column[Float]("longitude")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def updatedTimestamp = column[Option[DateTime]]("updated_timestamp")

  def deleted = column[Boolean]("deleted")

  override def * = (
    id,
    businessName,
    address1,
    address2,
    zipCode,
    suffixZipCode,
    state,
    city,
    country,
    phoneNumber,
    latitude,
    longitude,
    createdTimestamp,
    updatedTimestamp,
    deleted) <>(Restaurant.tupled, Restaurant.unapply)
}

class Restaurants @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                            customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val restaurants = TableQuery[RestaurantTableDef]

  def add(restaurant: Restaurant): Future[Option[RestaurantOutbound]] = {

    db.run(
      ((restaurants returning restaurants.map(_.id)) += restaurant).flatMap(newId => {

        restaurants.filter(r => r.id === newId && r.deleted === false).map(
          r => (r.id,
            r.businessName,
            r.address1,
            r.address2,
            r.zipCode,
            r.suffixZipCode,
            r.state,
            r.city,
            r.country,
            r.phoneNumber,
            r.latitude,
            r.longitude,
            r.createdTimestamp,
            r.updatedTimestamp)).result.map(
            _.headOption.map {
              case (id,
              businessName,
              address1,
              address2,
              zipCode,
              suffixZipCode,
              state,
              city,
              country,
              phoneNumber,
              latitude,
              longitude,
              createdTimestamp,
              updatedTimestamp) =>
                RestaurantOutbound(
                  id,
                  businessName,
                  address1,
                  address2,
                  zipCode,
                  suffixZipCode,
                  state,
                  city,
                  country,
                  phoneNumber,
                  latitude,
                  longitude,
                  createdTimestamp,
                  updatedTimestamp)
            }
          )
      }).transactionally)
  }

  def delete(id: Long): Future[Int] = {
    db.run(restaurants.filter(_.id === id).map(u => u.deleted).update(true))
  }

  def listAll: Future[Seq[RestaurantOutbound]] = {
    db.run(restaurants.filter(_.deleted === false).map(r =>
      (
        r.id,
        r.businessName,
        r.address1,
        r.address2,
        r.zipCode,
        r.suffixZipCode,
        r.state,
        r.city,
        r.country,
        r.phoneNumber,
        r.latitude,
        r.longitude,
        r.createdTimestamp,
        r.updatedTimestamp)).result.map(
        _.seq.map {
          case (
            id,
            businessName,
            address1,
            address2,
            zipCode,
            suffixZipCode,
            state,
            city,
            country,
            phoneNumber,
            latitude,
            longitude,
            createdTimestamp,
            updatedTimestamp) =>
            RestaurantOutbound(
              id,
              businessName,
              address1,
              address2,
              zipCode,
              suffixZipCode,
              state,
              city,
              country,
              phoneNumber,
              latitude,
              longitude,
              createdTimestamp,
              updatedTimestamp)
        }
      )
    )
  }

  def retrieveRestaurant(id: Long): Future[Option[RestaurantOutbound]] = {
    db.run(restaurants.filter(u => u.id === id && u.deleted === false).map(
      r => (
        r.id,
        r.businessName,
        r.address1,
        r.address2,
        r.zipCode,
        r.suffixZipCode,
        r.state,
        r.city,
        r.country,
        r.phoneNumber,
        r.latitude,
        r.longitude,
        r.createdTimestamp,
        r.updatedTimestamp)).result.map(
        _.headOption.map {
          case (
            id,
            businessName,
            address1,
            address2,
            zipCode,
            suffixZipCode,
            state,
            city,
            country,
            phoneNumber,
            latitude,
            longitude,
            createdTimestamp,
            updatedTimestamp) =>
            RestaurantOutbound(
              id,
              businessName,
              address1,
              address2,
              zipCode,
              suffixZipCode,
              state,
              city,
              country,
              phoneNumber,
              latitude,
              longitude,
              createdTimestamp,
              updatedTimestamp)
        }
      ))
  }

  def patchRestaurant(restaurant: Restaurant): Future[Option[RestaurantOutbound]] = {

    db.run(
      restaurants.filter(r =>
        r.id === restaurant.id && r.deleted === false).map(r =>
        (
          r.businessName,
          r.address1,
          r.address2,
          r.zipCode,
          r.suffixZipCode,
          r.state,
          r.city,
          r.country,
          r.phoneNumber,
          r.latitude,
          r.longitude,
          r.updatedTimestamp)).update(
          restaurant.businessName,
          restaurant.address1,
          restaurant.address2,
          restaurant.zipCode,
          restaurant.suffixZipCode,
          restaurant.state,
          restaurant.city,
          restaurant.country,
          restaurant.phoneNumber,
          restaurant.latitude,
          restaurant.longitude,
          restaurant.updatedTimestamp
        ).flatMap(x => {

        restaurants.filter(u => u.id === restaurant.id && u.deleted === false).map(
          r => (
            r.id,
            r.businessName,
            r.address1,
            r.address2,
            r.zipCode,
            r.suffixZipCode,
            r.state,
            r.city,
            r.country,
            r.phoneNumber,
            r.latitude,
            r.longitude,
            r.createdTimestamp,
            r.updatedTimestamp)).result.map(
            _.headOption.map {
              case (
                id,
                businessName,
                address1,
                address2,
                zipCode,
                suffixZipCode,
                state,
                city,
                country,
                phoneNumber,
                latitude,
                longitude,
                createdTimestamp,
                updatedTimestamp) =>
                RestaurantOutbound(
                  id,
                  businessName,
                  address1,
                  address2,
                  zipCode,
                  suffixZipCode,
                  state,
                  city,
                  country,
                  phoneNumber,
                  latitude,
                  longitude,
                  createdTimestamp,
                  updatedTimestamp)
            }
          )

      }).transactionally)
  }
}