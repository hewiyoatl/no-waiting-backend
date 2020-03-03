package utilities

import java.sql.Timestamp

import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._

object DateTimeMapper {

  implicit def date2dateTime = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    date => new DateTime(date)
  )

}
