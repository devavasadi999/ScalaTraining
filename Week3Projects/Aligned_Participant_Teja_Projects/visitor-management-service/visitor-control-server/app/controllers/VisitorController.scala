package controllers

import models.Visitor
import models.VisitorIdentityProof
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import services.VisitorService
import services.EmployeeService
import play.api.libs.json._
import utils.Validation
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VisitorController @Inject()(
  val cc: ControllerComponents,
  visitorService: VisitorService,
  employeeService: EmployeeService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def checkInVisitor(): Action[MultipartFormData[TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    // Extracting form data fields
    val name = request.body.dataParts.get("name").flatMap(_.headOption).getOrElse("")
    val hostName = request.body.dataParts.get("hostName").flatMap(_.headOption).getOrElse("")
    val hostMail = request.body.dataParts.get("hostMail").flatMap(_.headOption).getOrElse("")
    val building = request.body.dataParts.get("building").flatMap(_.headOption).getOrElse("")
    val email = request.body.dataParts.get("email").flatMap(_.headOption).getOrElse("")
    val contactNumber = request.body.dataParts.get("contactNumber").flatMap(_.headOption).getOrElse("")

    // Check for missing required fields and return a BadRequest if any are missing
    if (name.isEmpty || hostName.isEmpty || hostMail.isEmpty || building.isEmpty || email.isEmpty || contactNumber.isEmpty) {
      Future.successful(BadRequest(Json.obj("message" -> "Missing required fields")))
    } else {
      // Validate email format using the Validation object
      Validation.validateEmail(email) match {
        case Some(error) =>
          // Return a BadRequest with error message if email validation fails
          Future.successful(BadRequest(Json.obj("message" -> error.message)))
        case None =>
          Validation.validateContactNumber(contactNumber) match {
            case Some(error) =>
              // Return a BadRequest with error message if contact number validation fails
              Future.successful(BadRequest(Json.obj("message" -> error.message)))
            case None =>
              request.body.file("identityProof") match {
                case Some(filePart) =>
                  // Convert the uploaded file to a Blob (byte array)
                  val file = filePart.ref
                  val byteArray = java.nio.file.Files.readAllBytes(file.path)

                  // Create the Visitor object with all necessary fields, including the byteArray for identityProof
                  val visitor = Visitor(
                    name = name,
                    hostName = hostName,
                    hostMail = hostMail,
                    building = building,
                    email = email,
                    contactNumber = contactNumber
                  )

                  // Validate if host email is valid
                  employeeService.isEmployeeEmailValid(hostMail).flatMap { isValid =>
                    if (isValid) {
                      // Proceed to check-in the visitor and add identity proof
                      visitorService.checkIn(visitor).flatMap { createdVisitorId =>
                        val visitorIdentity = VisitorIdentityProof(
                          visitorId = createdVisitorId, // Use the visitorId returned from the check-in
                          identityProof = byteArray      // The byteArray representing the identity proof file
                        )

                        // Add the identity proof for the visitor
                        visitorService.addVisitorIdentity(visitorIdentity).map { _ =>
                          // Once both the visitor and identity proof are saved, return the success response
                          Ok(Json.toJson(s"Check-in and identity proof added successfully for $name, visitor_id: $createdVisitorId"))
                        }
                      }
                    } else {
                      // Return a bad request if the hostMail is invalid
                      Future.successful(BadRequest(Json.obj("message" -> "Invalid host email provided")))
                    }
                  }

                case None =>
                  // Return a BadRequest if no identity proof file is provided
                  Future.successful(BadRequest(Json.obj("message" -> "Missing identity proof file")))
              }
          }
      }
    }
  }



  def checkOutVisitor(visitorId: Long): Action[AnyContent] = Action.async {
    visitorService.checkOut(visitorId).map {
      case true => Ok("Visitor checked out successfully.")
      case false => InternalServerError("Failed to check out the visitor.")
    }
  }

  def list(): Action[AnyContent] = Action.async{
    visitorService.list().map(visitors => Ok(Json.toJson(visitors)))
  }

  def getVisitorDetails(visitorId: Long): Action[AnyContent] = Action.async{
    visitorService.get(visitorId).map {
      case Some(visitor) => Ok(Json.toJson(visitor))
      case None => NotFound(Json.obj("message" -> s"visitor with id $visitorId not found"))
    }
  }
}
