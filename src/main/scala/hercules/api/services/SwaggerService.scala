package hercules.api.services

import com.gettyimages.spray.swagger._
import com.wordnik.swagger.model.ApiInfo

import scala.reflect.runtime.universe._

trait SwaggerService extends SwaggerHttpService {

  override def apiTypes = Seq(typeOf[StatusService], typeOf[DemultiplexingService])
  override def apiVersion = "2.0"
  override def baseUrl = "http://localhost:8001"
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