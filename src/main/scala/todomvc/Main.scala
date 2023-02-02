package todomvc

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.Literals.host
import com.comcast.ip4s.{host, port}
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, CORSPolicy}

object Main extends IOApp.Simple {

  private val hostName = host"127.0.0.1"
  private val port     = port"8080"

  private val endpoints                          = new Endpoints(basePath = "todo")
  private val implementation: Implementation[IO] = new Implementation[IO](port, hostName, endpoints)

  protected val corsConfig: CORSPolicy =
    CORS.policy.withAllowOriginAll.withAllowMethodsAll

  private val routes: HttpRoutes[IO] = corsConfig(implementation.routes)

  override def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(hostName)
      .withPort(port)
      .withHttpApp(routes.orNotFound)
      .build
      .useForever
}
