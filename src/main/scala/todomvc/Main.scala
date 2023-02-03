package todomvc

import cats.effect.{IO, IOApp, Ref}
import cats.syntax.all.*
import com.comcast.ip4s.{host, port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, CORSPolicy}

import java.util.UUID

object Main extends IOApp.Simple:

  private val hostName = host"127.0.0.1"
  private val port     = port"8080"

  private val endpoints = new Endpoints(basePath = "todo")

  protected val corsConfig: CORSPolicy =
    CORS.policy.withAllowOriginAll.withAllowMethodsAll

  override def run: IO[Unit] =
    for
      todos          <- Ref[IO].of(Map.empty[UUID, Todo])
      implementation <- Implementation[IO](port, hostName, endpoints, todos).pure[IO]
      routes         <- corsConfig(implementation.routes).orNotFound.pure[IO]
      _ <- EmberServerBuilder
        .default[IO]
        .withHost(hostName)
        .withPort(port)
        .withHttpApp(routes)
        .build
        .useForever
    yield ()
end Main
