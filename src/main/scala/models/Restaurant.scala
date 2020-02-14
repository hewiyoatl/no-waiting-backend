package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import utilities.DateTimeMapper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Restaurant(id: Option[Long],
                      address1: Option[String],
                      address2: Option[String],
                      zipCode: Option[String],
                      state: Option[String],
                      city: Option[String],
                      country: Option[String],
                      phoneNumber: Option[String],
                      latitude: Option[Float],
                      longitude: Option[Float],
                      createdTimestamp: Option[DateTime],
                      deleted: Boolean)

case class RestaurantOutbound(id: Option[Long],
                              address1: Option[String],
                              address2: Option[String],
                              zipCode: Option[String],
                              state: Option[String],
                              city: Option[String],
                              country: Option[String],
                              phoneNumber: Option[String],
                              latitude: Option[Float],
                              longitude: Option[Float],
                              createdTimestamp: Option[DateTime])

case class RestaurantFormData(firstName: String, lastName: String, mobile: String, email: String)

object RestaurantForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "mobile" -> nonEmptyText,
      "email" -> email
    )(RestaurantFormData.apply)(RestaurantFormData.unapply)
  )
}

class RestaurantTableDef(tag: Tag) extends Table[Restaurant](tag, Some("talachitas"),"restaurant") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def address1 = column[Option[String]]("address_1")

  def address2 = column[Option[String]]("address_2")

  def zipCode = column[Option[String]]("zip_code")

  def state = column[Option[String]]("state")

  def city = column[Option[String]]("city")

  def country = column[Option[String]]("country")

  def phoneNumber = column[Option[String]]("phone_number")

  def latitude = column[Option[Float]]("latitude")

  def longitude = column[Option[Float]]("longitud")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def deleted = column[Boolean]("deleted")

  override def * = (
    id,
    address1,
    address2,
    zipCode,
    state,
    city,
    country,
    phoneNumber,
    latitude,
    longitude,
    createdTimestamp,
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
            r.address1,
            r.address2,
            r.zipCode,
            r.state,
            r.city,
            r.country,
            r.phoneNumber,
            r.latitude,
            r.longitude,
            r.createdTimestamp)).result.map(
            _.headOption.map {
              case (id,
              address1,
              address2,
              zipCode,
              state,
              city,
              country,
              phoneNumber,
              latitude,
              longitude,
              createdTimestamp) =>
                RestaurantOutbound(
                  id,
                  address1,
                  address2,
                  zipCode,
                  state,
                  city,
                  country,
                  phoneNumber,
                  latitude,
                  longitude,
                  createdTimestamp)
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
        r.address1,
        r.address2,
        r.zipCode,
        r.state,
        r.city,
        r.country,
        r.phoneNumber,
        r.latitude,
        r.longitude,
        r.createdTimestamp)).result.map(
        _.seq.map {
          case (
            id,
            address1,
            address2,
            zipCode,
            state,
            city,
            country,
            phoneNumber,
            latitude,
            longitude,
            createdTimestamp) =>
            RestaurantOutbound(
              id,
              address1,
              address2,
              zipCode,
              state,
              city,
              country,
              phoneNumber,
              latitude,
              longitude,
              createdTimestamp)
        }
      )
    )
  }

  def retrieveRestaurant(id: Long): Future[Option[RestaurantOutbound]] = {
    db.run(restaurants.filter(u => u.id === id && u.deleted === false).map(
      r => (
        r.id,
        r.address1,
        r.address2,
        r.zipCode,
        r.state,
        r.city,
        r.country,
        r.phoneNumber,
        r.latitude,
        r.longitude,
        r.createdTimestamp)).result.map(
        _.headOption.map {
          case (
            id,
            address1,
            address2,
            zipCode,
            state,
            city,
            country,
            phoneNumber,
            latitude,
            longitude,
            createdTimestamp) =>
            RestaurantOutbound(
              id,
              address1,
              address2,
              zipCode,
              state,
              city,
              country,
              phoneNumber,
              latitude,
              longitude,
              createdTimestamp)
        }
      ))
  }

  def patchRestaurant(restaurant: Restaurant): Future[Option[RestaurantOutbound]] = {

    db.run(
      restaurants.filter(r =>
        r.id === restaurant.id && r.deleted === false).map(r =>
        (
          r.address1,
          r.address2,
          r.zipCode,
          r.state,
          r.city,
          r.country,
          r.phoneNumber,
          r.latitude,
          r.longitude)).update(
          restaurant.address1,
          restaurant.address2,
          restaurant.zipCode,
          restaurant.state,
          restaurant.city,
          restaurant.country,
          restaurant.phoneNumber,
          restaurant.latitude,
          restaurant.longitude
        ).flatMap(x => {

        restaurants.filter(u => u.id === restaurant.id && u.deleted === false).map(
          r => (
            r.id,
            r.address1,
            r.address2,
            r.zipCode,
            r.state,
            r.city,
            r.country,
            r.phoneNumber,
            r.latitude,
            r.longitude,
            r.createdTimestamp)).result.map(
            _.headOption.map {
              case (
                id,
                address1,
                address2,
                zipCode,
                state,
                city,
                country,
                phoneNumber,
                latitude,
                longitude,
                createdTimestamp) =>
                RestaurantOutbound(
                  id,
                  address1,
                  address2,
                  zipCode,
                  state,
                  city,
                  country,
                  phoneNumber,
                  latitude,
                  longitude,
                  createdTimestamp)
            }
          )

      }).transactionally)

  }

}