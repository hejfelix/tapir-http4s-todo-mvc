package tapir.todomvc
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import org.http4s.implicits._

object Main extends IOApp {

  private val hostName = "127.0.0.1"
  private val port     = 8080

  private val endpoints      = new Endpoints()
  private val implementation = new Implementation[IO](port, hostName, endpoints)

  protected val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  def isAsset(asset: WebjarAsset): Boolean =
    List(".js", ".css", ".html").exists(asset.asset.endsWith)

  val webjars: HttpRoutes[IO] = webjarService(Config(blockingExecutionContext = global, filter = isAsset))

  override def run(args: List[String]): IO[ExitCode] = {

    val _ = args // We don't need these

    val combinedRoutes: HttpRoutes[IO] = webjars <+> implementation.routes
    val routes                         = CORS(combinedRoutes, corsConfig)

    BlazeServerBuilder[IO]
      .bindHttp(port, hostName)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .lastOrError
  }
}
