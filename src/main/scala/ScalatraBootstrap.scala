import br.com.longhi.hotel._
import br.com.longhi.hotel.databases.SlickDatabase
import br.com.longhi.hotel.routes.{FluxoRoute, HospedeRoute, QuartoRoute}
import jakarta.servlet.ServletContext
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ControleReservasSwagger

  override def init(context: ServletContext): Unit = {
    SlickDatabase.init()

    context.mount(new QuartoRoute, "/quartos/*")
    context.mount(new FluxoRoute, "/fluxo/*")
    context.mount(new HospedeRoute, "/hospedes/*")
    context.mount(new ResourcesApp(), "/api-docs")
  }

  override def destroy(context: ServletContext) = {
    super.destroy(context)
  }
}