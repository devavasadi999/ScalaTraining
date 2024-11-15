package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EquipmentTypeRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val equipmentTypes = TableQuery[EquipmentTypeTable]

  def list(): Future[Seq[EquipmentType]] = db.run(equipmentTypes.result)

  def find(id: Long): Future[Option[EquipmentType]] = db.run(equipmentTypes.filter(_.id === id).result.headOption)

  def add(equipmentType: EquipmentType): Future[EquipmentType] = {
    val action = (equipmentTypes returning equipmentTypes.map(_.id)
      into ((eqType, id) => eqType.copy(id = id))
      ) += equipmentType

    db.run(action)
  }

  def update(equipmentType: EquipmentType): Future[Int] = db.run(equipmentTypes.filter(_.id === equipmentType.id).update(equipmentType))

  def delete(id: Long): Future[Int] = db.run(equipmentTypes.filter(_.id === id).delete)
}
