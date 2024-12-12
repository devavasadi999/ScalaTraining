package case_study_1.IntegrationTests

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funsuite.AnyFunSuite
import akka.http.scaladsl.server.Route
import case_study_1.AggregatedDataApi

class AggregatedMetricsAPITest extends AnyFunSuite with ScalatestRouteTest {

  // Use the actual API route
  val route: Route = AggregatedDataApi.route

  test("GET /api/aggregated-data should return a response with count > 0") {
    // Make a real GET request to the API
    Get("/api/aggregated-data") ~> route ~> check {
      // Ensure the status code is OK
      assert(status == StatusCodes.OK)

      // Parse response and check count
      val responseJson = responseAs[String]
      import spray.json._
      import spray.json.DefaultJsonProtocol._

      // Convert JSON string to a list of JsObject
      val responseData = responseJson.parseJson.convertTo[List[JsObject]]
      assert(responseData.nonEmpty, "Response data count should be greater than zero")
    }
  }
}
