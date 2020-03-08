package models

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

case class Address(id: Option[Long],
                   address1: String,
                   address2: String,
                   zipCode: String,
                   suffixZipCode: String,
                   state: String,
                   city: String,
                   country: String,
                   latitude: String,
                   longitude: String,
                   createdTimestamp: Option[DateTime],
                   deleted: Boolean)

case class AddressOutbound(id: Option[Long],
                           address1: String,
                           address2: String,
                           zipCode: String,
                           suffixZipCode: String,
                           state: String,
                           city: String,
                           country: String,
                           latitude: String,
                           longitude: String,
                           createdTimestamp: Option[DateTime])

//needed to parse dates for slick
import utilities.DateTimeMapper._

class AddressTableDef(tag: Tag) extends Table[Address](tag, Some("talachitas"), "address") {

  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

  def address1 = column[String]("address_1")

  def address2 = column[String]("address_2")

  def zipCode = column[String]("zip_code")

  def suffixZipCode = column[String]("suffix_zip_code")

  def state = column[String]("state")

  def city = column[String]("city")

  def country = column[String]("country")

  def latitude = column[String]("latitude")

  def longitude = column[String]("longitude")

  def createdTimestamp = column[Option[DateTime]]("created_timestamp")

  def deleted = column[Boolean]("deleted")

  override def * =
     (id,
      address1,
      address2,
      zipCode,
      suffixZipCode,
      state,
      city,
      country,
      latitude,
      longitude,
      createdTimestamp,
      deleted) <>(Address.tupled, Address.unapply)
}

class Addresses @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                          config: Configuration,
                          customizedSlickConfig: CustomizedSlickConfig)
  extends HasDatabaseConfigProviderTalachitas[JdbcProfile] {

  val logger: Logger = Logger(this.getClass())

  this.dbConfig = customizedSlickConfig.createDbConfigCustomized(dbConfigProvider)

  val addresses = TableQuery[AddressTableDef]

  val timeoutDatabaseSeconds = config.get[Duration]("talachitas.dbs.timeout")

  def add(adr: Address): Option[Long] = {

    val q1 = (addresses returning addresses.map(_.id)) += adr

    val future: Future[Option[Long]] = db.run { q1.transactionally }

    val response: Option[Long] = Await.result(future, timeoutDatabaseSeconds)
    response
  }

  def delete(id: Long): Future[Int] = {
    db.run(addresses.filter(_.id === id).map(u => u.deleted).update(true).transactionally)
  }

  def receivingPrimaryKeyQuestion(addressIn: Option[Address]): Boolean = {

    if (addressIn.map(_.address1).getOrElse("") == "") {
      false
    }

    if (addressIn.map(_.address2).getOrElse("") == "") {
      false
    }

    if (addressIn.map(_.city).getOrElse("") == ""){
      false
    }

    if (addressIn.map(_.state).getOrElse("") == ""){
      false
    }

    if (addressIn.map(_.zipCode).getOrElse("") == "") {
      false
    }

    if (addressIn.map(_.suffixZipCode).getOrElse("") == "") {
      false
    }

    if (addressIn.map(_.country).getOrElse("") == "") {
      false
    }

    if (addressIn.map(_.latitude).getOrElse(0) == 0) {
      false
    }

    if (addressIn.map(_.longitude).getOrElse(0) == 0) {
      false
    }
    true
  }
  def retrieveAddressPerPrimaryKey(addressIn: Option[Address]): Option[AddressOutbound] = {

    //primary key
    // address 1, address 2,
    // city, state, zip code, suffix zip code,
    // country,

    // latitude, longitude

    val future: Future[Option[AddressOutbound]] =
      db.run(addresses
        .filter(addresses => addresses.address1 === addressIn.map(_.address1))
        .filter(addresses => addresses.address2 === addressIn.map(_.address2))
        .filter(addresses => addresses.city === addressIn.map(_.city))
        .filter(addresses => addresses.state === addressIn.map(_.state))
        .filter(addresses => addresses.zipCode === addressIn.map(_.zipCode))
        .filter(addresses => addresses.suffixZipCode === addressIn.map(_.suffixZipCode))
        .filter(addresses => addresses.country === addressIn.map(_.country))
        .filter(addresses => addresses.latitude === addressIn.map(_.latitude))  // decimals have only 4 positions in mysql and google send 5 decimals
        .filter(addresses => addresses.longitude === addressIn.map(_.longitude)) // decimals have only 4 positions in mysql and google send 5 decimals
      .map(
        record => (
          record.id,
          record.address1,
          record.address2,
          record.zipCode,
          record.suffixZipCode,
          record.state,
          record.city,
          record.country,
          record.latitude,
          record.longitude,
          record.createdTimestamp,
          record.deleted)).result.map(
      _.headOption.map {
        case (
          id,
          address1,
          address2,
          zipCode,
          suffixZipCode,
          state,
          city,
          country,
          latitude,
          longitude,
          createdTimestamp,
          deleted
          ) =>
          AddressOutbound(
            id,
            address1,
            address2,
            zipCode,
            suffixZipCode,
            state,
            city,
            country,
            latitude,
            longitude,
            createdTimestamp
          )
      }
    ).transactionally)

    val response: Option[AddressOutbound] = Await.result(future, timeoutDatabaseSeconds)
    response
  }
}