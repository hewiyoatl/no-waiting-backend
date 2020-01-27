package models

import com.typesafe.config.ConfigValueFactory
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import services.EncryptDecryptService
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.JdbcProfile

trait HasDatabaseConfigTalachitas[P <: BasicProfile] {
  /** The Slick database configuration. */
  var dbConfig: DatabaseConfig[P] // field is declared as a val because we want a stable identifier.
  /** The Slick profile extracted from `dbConfig`. */
  protected final lazy val profile: P = dbConfig.profile // field is lazy to avoid early initializer problems.
  @deprecated("Use `profile` instead of `driver`", "2.1")
  protected final lazy val driver: P = dbConfig.profile // field is lazy to avoid early initializer problems.
  /** The Slick database extracted from `dbConfig`. */
  protected final def db: P#Backend#Database = dbConfig.db
}

trait HasDatabaseConfigProviderTalachitas[P <: BasicProfile] extends HasDatabaseConfigTalachitas[P] {
  /** The provider of a Slick `DatabaseConfig` instance.*/
  protected val dbConfigProvider: DatabaseConfigProvider
  override var dbConfig: DatabaseConfig[P] = dbConfigProvider.get[P] // field is lazy to avoid early initializer problems.
}

import play.api.Configuration

class CustomizedSlickConfig @Inject()(config: Configuration,
                                      encryptDecryptService: EncryptDecryptService) {

  val logger: Logger = Logger(this.getClass())

  def createDbConfigCustomized (dbConfigProvider: DatabaseConfigProvider) : DatabaseConfig[JdbcProfile] = {

    val user: String = encryptDecryptService.decrypt(config.get[String]("slick.dbs.default.db.user"))
    val pass: String = encryptDecryptService.decrypt(config.get[String]("slick.dbs.default.db.password"))

    val dbConfigOwn = dbConfigProvider.get[JdbcProfile]
    val decryptedConfig = dbConfigOwn.config
      .withValue("db.user", ConfigValueFactory.fromAnyRef(user))
      .withValue("db.password", ConfigValueFactory.fromAnyRef(pass))

    DatabaseConfig.forConfig[JdbcProfile]("", decryptedConfig)

  }
}
