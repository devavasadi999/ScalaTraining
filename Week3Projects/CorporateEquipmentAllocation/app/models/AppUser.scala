package models

import slick.jdbc.PostgresProfile.api._ // For database operations and Postgres support
import slick.lifted.{ProvenShape, TableQuery, Tag} // For table definitions and queries
import scala.language.implicitConversions // For Slick's implicit conversions


case class AppUser(id: Long, username: String, password: String)

class AppUserTable(tag: Tag) extends Table[AppUser](tag, "app_user") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username", O.Unique)
  def password = column[String]("password") // Encrypted password

  def * = (id, username, password) <> ((AppUser.apply _).tupled, AppUser.unapply)
}
