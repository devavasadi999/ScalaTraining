package controllers

import javax.inject._
import models.{Person, PersonDAO}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonController @Inject()(personDAO: PersonDAO, cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  val personForm: Form[CreatePersonForm] = Form {
    mapping(
      "sno"-> nonEmptyText,
      "name" -> nonEmptyText,
      "city" -> nonEmptyText
    )(CreatePersonForm.apply)(CreatePersonForm.unapply)
  }

  def index = Action.async { implicit request =>
    personDAO.list().map { persons =>
      Ok(views.html.people(persons))
    }
  }

  def addPerson = Action.async { implicit request =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val sno = args("sno").head.toInt
      val name = args("name").head
      val city = args("city").head
      personDAO.create(sno,name, city).map { _ =>
        Redirect(routes.PersonController.index)
      }
    }.getOrElse(Future.successful(Redirect(routes.PersonController.index)))
  }


}

case class CreatePersonForm(sno: String,name: String, city: String)
