import br.com.longhi.hotel._
import br.com.longhi.hotel.routes.{FluxoRoute, HospedeRoute, QuartoRoute}
import jakarta.servlet.ServletContext
import org.scalatra._
import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ControleReservasSwagger

  override def init(context: ServletContext): Unit = {
    val db = Database
      .forURL(
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        executor = AsyncExecutor("test", numThreads = 10, queueSize = 1000),
        driver = "org.h2.Driver")

    Await.result(db.run(Tables.criarSchema()), 7.seconds)
    Await.result(db.run(Tables.popularBanco()), Duration.Inf)

    context.mount(new QuartoRoute(db), "/quartos/*")
    context.mount(new FluxoRoute(db), "/fluxo/*")
    context.mount(new HospedeRoute(db), "/hospedes/*")
    context.mount(new ResourcesApp(), "/api-docs")
  }

  override def destroy(context: ServletContext) = {
    super.destroy(context)
  }
}