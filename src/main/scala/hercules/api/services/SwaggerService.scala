package hercules.api.services

import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo

import scala.reflect.runtime.universe._

/**
 * The SwaggerService trait extends the SwaggerHttpService and defines the endpoints and contents of the Swagger documentation.
 */
trait SwaggerService extends SwaggerHttpService {

  /**
   * A list of the API services to include in the documentation
   */
  override def apiTypes =
    Seq(
      typeOf[StatusService]
    // Temporarily removed. /JD 20150507
    //typeOf[DemultiplexingService]
    )
  override def apiVersion = "2.0"
  override def baseUrl = "http://localhost:8001"
  /**
   * The endpoint where the api-docs are exposed
   */
  override def docsPath = "api-docs"
  override def apiInfo = Some(
    new ApiInfo(
      title = "Hercules REST API",
      description = "REST API for controlling key Hercules operations.",
      termsOfServiceUrl = "https://github.com/Molmed/hercules",
      contact = "Pontus Larsson @b97pla",
      license = "MIT",
      licenseUrl = "https://github.com/Molmed/hercules")
  )

  /**
   * The routes handled by the SwaggerService include the dynamically generated endpoints (the ``routes`` property of the SwaggerHttpService),
   * as well as endpoints for accessing the static API docs.
   */
  def route = routes ~
    get {
      pathPrefix("") {
        pathEndOrSingleSlash {
          getFromResource("docs/index.html")
        }
      } ~
        getFromResourceDirectory("docs")
    }
}