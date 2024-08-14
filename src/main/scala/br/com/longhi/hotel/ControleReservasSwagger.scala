package br.com.longhi.hotel

import org.scalatra.CorsSupport.{CORSConfig, CorsConfigKey}
import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ControleReservasSwagger extends Swagger(
  Swagger.SpecVersion,
  "1.0",
  ApiInfo(
    title = "Controle de reservas API",
    description = "API para gerenciar quartos e hóspedes",
    termsOfServiceUrl = "http://example.com/terms/",
    contact = ContactInfo("Gabriel", "", "gabriellonghi07@gmail.com"),
    license = LicenseInfo("Apache 2.0", "http://www.apache.org/licenses/LICENSE-2.0.html")))

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase {

  override def initialize(config: ConfigT): Unit = {
    val corsCfg = CORSConfig(
      allowedOrigins = Seq.empty,
      allowedMethods = Seq.empty,
      allowedHeaders = Seq.empty,
      allowCredentials = false,
      preflightMaxAge = 0,
      enabled = false
    )

    config.context.update(CorsConfigKey, corsCfg)
    super.initialize(config)
  }

}
