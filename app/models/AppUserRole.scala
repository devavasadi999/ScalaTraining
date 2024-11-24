package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.language.implicitConversions
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc._

object Role extends Enumeration {
  type Role = Value
  val EventManager, EntertainmentTeam, LogisticsTeam, CateringTeam, DecorationsTeam = Value

  implicit val roleFormat: Format[Role] = Json.formatEnum(this)

  // Slick MappedColumnType for Role Enumeration
  implicit val roleMapper: JdbcType[Role] with BaseTypedType[Role] =
    MappedColumnType.base[Role, String](
      _.toString,
      Role.withName
    )
}

case class AppUserRole(appUserId: Long, role: Role.Role)

class AppUserRoleTable(tag: Tag) extends Table[AppUserRole](tag, "app_user_role") {
  def appUserId = column[Long]("app_user_id")
  def role = column[Role.Role]("role")

  def * = (appUserId, role) <> ((AppUserRole.apply _).tupled, AppUserRole.unapply)

  def userFK = foreignKey("user_fk", appUserId, TableQuery[AppUserTable])(_.id, onDelete = ForeignKeyAction.Cascade)
}
