// controllers/PeopleController.scala
package controllers

import javax.inject._
import models.{Person, PersonRepository}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PeopleController @Inject()(personRepository: PersonRepository, cc: ControllerComponents,ws: WSClient)(implicit ec: ExecutionContext) extends AbstractController(cc) {
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
        personRepository.add(person).flatMap { _ =>
          callExternalApi(person)
        }.map { apiResponse =>
          Created(Json.toJson(person)).withHeaders("API-Response" -> apiResponse)
        }.recover {
          case e: Exception => InternalServerError("Failed to call external API")
        }
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

  def callExternalApi(person: Person): Future[String] = {
    val url = "http://34.16.199.130:8888/write-message"
    val jsonData = Json.obj(
      "sno" -> person.sno,
      "name" -> person.name,
      "city" -> person.city
    )
    println(s"Sending request to $url with payload $jsonData")
    println(s"this is working: api being called")
    ws.url(url)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(jsonData)
      .map { response =>
        response.status match {
          case 200 => { {
            println("successful")
            response.body
          }
          }
          case _ => {
            println(response)
            println(response.status)
            println("API called failed")
            "API call failed"
          }

        }
      }
  }
}
