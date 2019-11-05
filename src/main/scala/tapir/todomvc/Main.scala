package tapir.todomvc
import java.util.UUID

import cats.data.EitherT
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

  type Stack[T] = EitherT[IO, String, T]

  implicit val log: Log[Stack]     = Log.default[Stack]
  implicit val idGen: IdGen[Stack] = IdGen.default[Stack]

  import cats.mtl._
  import cats.mtl.implicits._

  val bla: FunctorRaise[Stack, String] = implicitly[FunctorRaise[Stack, String]]

  private val implementationF: EitherT[IO, String, Implementation[Stack]] =
    for {
      storeRef <- Ref.of[Stack, Map[UUID, Todo]](Map.empty[UUID, Todo])
      store = TodoStore.todoStore(storeRef)
    } yield {
      implicit val st: TodoStore[Stack] = store
      new Implementation[Stack](port, hostName, endpoints)
    }

  protected val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  def isAsset(asset: WebjarAsset): Boolean =
    List(".js", ".css", ".html").exists(asset.asset.endsWith)

  val webjars: HttpRoutes[IO] = webjarService(Config(blockingExecutionContext = global, filter = isAsset))

  val fToG: Stack ~> IO = new (Stack ~> IO) {
    override def apply[A](fa: Stack[A]): IO[A] =
      fa.leftMap(s => new Throwable(s)).rethrowT
  }

  val program: EitherT[IO, String, HttpApp[IO]] = for {
    implementation <- implementationF
    routes  = Routing.route[Stack, IO](endpoints, implementation, fToG)
    routing = webjars <+> routes
  } yield CORS(routing, corsConfig).orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    program.foldF(
      _ => ExitCode.Error.pure[IO],
      app =>
        BlazeServerBuilder[IO]
          .bindHttp(port, hostName)
          .withHttpApp(app)
          .serve
          .compile
          .lastOrError
    )
}
