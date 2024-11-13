package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

case class Person(sno: Int, name: String, city: String)



class PersonDAO @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class PersonTable(tag: Tag) extends Table[Person](tag, "person") {
    def sno = column[Int]("sno", O.PrimaryKey)
    def name = column[String]("name")
    def city = column[String]("city")
    def * = (sno, name, city) <> ((Person.apply _).tupled, Person.unapply)
  }


  val persons = TableQuery[PersonTable]

  def list(): Future[Seq[Person]] = db.run {
    persons.result
  }

  def create(sno: Int,name: String, city: String): Future[Person] = db.run {
    (persons += Person(sno, name, city)).map(_ => ( Person(sno, name, city)))
  }


  def update(sno: Int, name: String, city: String): Future[Unit] = db.run {
    persons.filter(_.sno === sno).update(Person(sno, name, city)).map(_ => ())
  }

  def delete(sno: Int): Future[Unit] = db.run {
    persons.filter(_.sno === sno).delete.map(_ => ())
  }
}
/*
     db.run  executes the blocks
     in the form of asynchronous task and
     returns future objects

     for executing the asynchronous task
     concurrency execution context need
     to be there as part of the current object cont

*/
