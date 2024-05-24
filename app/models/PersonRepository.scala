// models/PersonRepository.scala
package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{Future, ExecutionContext}

@Singleton
class PersonRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._



  private  class PersonTable(tag: Tag) extends Table[Person](tag, "person") {
    def sno = column[Int]("sno", O.PrimaryKey)
    def name = column[String]("name")
    def city = column[String]("city")
    def * = (sno, name, city) <> ((Person.apply _).tupled, Person.unapply)
  }

  private val persons = TableQuery[PersonTable]

  def list(): Future[Seq[Person]] = db.run(persons.result)

  def find(sno: Int): Future[Option[Person]] = db.run(persons.filter(_.sno === sno).result.headOption)

  def add(person: Person): Future[Int] = db.run(persons += person)

  def update(person: Person): Future[Int] = db.run(persons.filter(_.sno === person.sno).update(person))

  def delete(sno: Int): Future[Int] = db.run(persons.filter(_.sno === sno).delete)
}

// Assuming PersonTable class and Person case class are defined elsewhere in your models package
