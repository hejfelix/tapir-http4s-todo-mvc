package tapir.todomvc
import java.nio.charset.StandardCharsets

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.http4s.syntax.kleisli._

import scala.concurrent.duration._

object Main extends IOApp {

  private val hostName = "127.0.0.1"
  private val port     = 8080
  private val charset  = StandardCharsets.UTF_8

  private val endpoints      = new Endpoints(charset)
  private val implementation = new Implementation[IO](port, hostName, endpoints)

  protected val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  override def run(args: List[String]): IO[ExitCode] = {

    val _ = args // We don't need these

    val combinedRoutes: HttpRoutes[IO] = implementation.routes
    val routes                         = CORS(combinedRoutes, corsConfig)

    BlazeServerBuilder[IO]
      .bindHttp(port, hostName)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .lastOrError
  }
}
