// controllers/PeopleController.scala
package controllers

import javax.inject._
import models.{Person, PersonRepository}
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PeopleController @Inject()(personRepository: PersonRepository, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val personFormat: Format[Person] = Json.format[Person]

  def getPeople = Action.async {
    personRepository.list().map { people =>
      Ok(Json.toJson(people))
    }
  }

  def getPerson(sno: Int) = Action.async {
    personRepository.find(sno).map {
      case Some(person) => Ok(Json.toJson(person))
      case None => NotFound
    }
  }

  def createPerson = Action.async(parse.json) { request =>
    request.body.validate[Person].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      person => {
        personRepository.add(person).map(_ => Created(Json.toJson(person)))
      }
    )
  }

  def updatePerson(sno: Int) = Action.async(parse.json) { request =>
    request.body.validate[Person].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      person => {
        personRepository.update(person).map(_ => Ok(Json.toJson(person)))
      }
    )
  }

  def deletePerson(sno: Int) = Action.async {
    personRepository.delete(sno).map(_ => NoContent)
  }
}
