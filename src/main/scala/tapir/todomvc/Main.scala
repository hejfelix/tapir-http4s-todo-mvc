package tapir.todomvc
import java.util.UUID

import cats.data.{EitherT, ReaderT}
import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import cats.~>
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}
import org.http4s.server.staticcontent.webjarService
import org.http4s.{HttpApp, HttpRoutes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends IOApp {

  private val hostName = "127.0.0.1"
  private val port     = 8080

  private val endpoints = new Endpoints(basePath = "todo")

  type Stack[T] = ReaderT[EitherT[IO, String, *], TodoConfig, T]

  implicit val log: Log[Stack]     = Log.default[Stack]
  implicit val idGen: IdGen[Stack] = IdGen.default[Stack]

  import cats.mtl.implicits._

  private val implementationF: ReaderT[EitherT[IO, String, *], TodoConfig, TodoStore[Stack]] =
    Ref.of[Stack, Map[UUID, Todo]](Map.empty[UUID, Todo]).map(TodoStore.default[Stack])

  protected val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  private val config = tapir.todomvc.TodoConfig("todo", hostName, port)

  def isAsset(asset: WebjarAsset): Boolean =
    List(".js", ".css", ".html").exists(asset.asset.endsWith)

  val webjars: HttpRoutes[IO] = webjarService(Config(blockingExecutionContext = global, filter = isAsset))

  val fToG: Stack ~> IO = new (Stack ~> IO) {
    override def apply[A](fa: Stack[A]): IO[A] =
      fa.run(config).leftMap(s => new Throwable(s)).rethrowT
  }

  val program: ReaderT[EitherT[IO, String, *], TodoConfig, HttpApp[IO]] = for {
    implicit0(implementation: TodoStore[Stack]) <- implementationF
    routes  = Routing.route[Stack, IO](endpoints, fToG)
    routing = webjars <+> routes
  } yield CORS(routing, corsConfig).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    program
      .run(config)
      .foldF(
        _ => ExitCode.Error.pure[IO],
        app =>
          BlazeServerBuilder[IO]
            .bindHttp(port, hostName)
            .withHttpApp(app)
            .serve
            .compile
            .lastOrError,
      )
}
