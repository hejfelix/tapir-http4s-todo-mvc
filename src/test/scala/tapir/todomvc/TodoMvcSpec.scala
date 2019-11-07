package tapir.todomvc
import java.util.UUID

import cats.data.{EitherT, ReaderT}
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.~>
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.scalatest.{Matchers, Outcome}
import tapir.Endpoint
import tapir.client.sttp._
import org.http4s.syntax.kleisli._
import org.http4s.server.staticcontent.WebjarService.Config
import org.http4s.server.staticcontent.webjarService
import tapir.todomvc.Main.isAsset
import cats.mtl.implicits._
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TodoMvcSpec extends org.scalatest.fixture.WordSpec with Matchers {

  private val baseUri = uri"http://127.0.0.1:8080"
  private val port    = 8080

  private val executionContext: ExecutionContext      = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(executionContext)
  private implicit val timer                          = IO.timer(executionContext)

  private implicit val catsSttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

  private val basepath  = "basepath"
  private val endpoints = new Endpoints(basepath)

  val config = TodoConfig(basepath, baseUri.host, port)

  type Stack[T] = ReaderT[EitherT[IO, String, *], TodoConfig, T]

  implicit val idGen: IdGen[Stack] = IdGen.default[Stack]

  private val implementationF: Stack[TodoStore[Stack]] =
    Ref.of[Stack, Map[UUID, Todo]](Map.empty[UUID, Todo]).map(TodoStore.default[Stack])

  private val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  val fToG: Stack ~> IO = new (Stack ~> IO) {
    override def apply[A](fa: Stack[A]): IO[A] =
      fa.run(config).leftMap(s => new Throwable(s)).rethrowT
  }

  val webjars: HttpRoutes[IO] = webjarService(Config(blockingExecutionContext = global, filter = isAsset))

  val program: ReaderT[EitherT[IO, String, *], TodoConfig, HttpApp[IO]] = for {
    implicit0(implementation: TodoStore[Stack]) <- implementationF
    routes  = Routing.route[Stack, IO](endpoints, fToG)
    routing = webjars <+> routes
  } yield CORS(routing, corsConfig).orNotFound

  val app = program.run(config).value.unsafeRunSync().right.get

  private def serverResource: Resource[IO, Server[IO]] =
    BlazeServerBuilder[IO]
      .bindHttp(port, baseUri.host)
      .withHttpApp(app)
      .resource

  class SyncRes[T](val code: StatusCode, res: => T, _response: Response[Either[String, T]]) {
    def body: T                               = res
    def response: Response[Either[String, T]] = _response
  }

  implicit class SendSync[T](req: Request[Either[String, T], Nothing]) {
    def sendSync: SyncRes[T] = {
      val response: Response[Either[String, T]] = req.send().unsafeRunSync()
      new SyncRes(response.code, response.body.map(_.right.get).right.get, response)
    }
  }
  implicit class TodoId(todo: Todo) {
    def id: UUID = UUID.fromString(todo.url.get.reverse.takeWhile(_ != '/').reverse)
  }

  val getTodos = endpoints.getEndpoint.toSttpRequest(baseUri).apply(())
  val delete   = endpoints.deleteEndpoint.toSttpRequest(baseUri).apply(())

  def getTodo(id: UUID): Request[Either[String, Todo], Nothing] = {
    val endpoint: Endpoint[UUID, String, Todo, Nothing] = endpoints.getTodoEndpoint
    endpoint.toSttpRequest(baseUri).apply(id)
  }

  def postTodo(todo: Todo): Request[Either[String, Todo], Nothing] =
    endpoints.postEndpoint
      .toSttpRequest(baseUri)
      .apply(todo)

  def patchTodo(id: UUID, patch: Todo): Request[Either[String, Todo], Nothing] =
    Function.untupled(endpoints.patchByIdEndpoint.toSttpRequest(baseUri))(id, patch)

  "The API Root" should {
    "responds to GET" in { _ =>
      getTodos.sendSync.code shouldBe StatusCodes.Ok
    }
    "responds to POST with the given TODO" in { _ =>
      val todo = Todo(Option("Title"), None, None, None)
      postTodo(todo).sendSync.body.title shouldBe todo.title
    }
    "responds successfully to DELETE" in { _ =>
      delete.sendSync.code shouldBe StatusCodes.Ok
    }
    "after DELETE, GET yields empty JSON array" in { _ =>
      delete.sendSync
      getTodos.sendSync.body shouldBe empty
    }
  }

  "Store new TODOS" should {
    "post new todo at root" in { _ =>
      val todo = Todo(Option("Title"), None, None, None)
      postTodo(todo).sendSync.body.url should not be postTodo(todo).sendSync.body.url
      getTodos.sendSync.body should have size 2
    }
    "work with existing todo" in { _ =>
      val todo                = Todo(Option("Title"), None, None, None)
      val sync: SyncRes[Todo] = postTodo(todo).sendSync
      val postedTodo          = sync.body
      getTodo(postedTodo.id).sendSync.body.title shouldBe todo.title
    }
  }

  "Tracking order" should {
    "create a todo with an order field" in { _ =>
      val orderId = Option(1337)
      val todo    = Todo(Option("Title"), None, None, orderId)
      postTodo(todo).sendSync.body.order shouldBe orderId
    }
    "PATCH a todo to change order id" in { _ =>
      val orderId      = Option(1337)
      val todo         = Todo(Option("Title"), None, None, orderId)
      val posted       = postTodo(todo).sendSync.body
      val patchOrderId = Option(42)
      patchTodo(posted.id, Todo(None, None, None, patchOrderId)).sendSync.body.order shouldBe patchOrderId
    }
    "remember patches" in { _ =>
      val orderId      = Option(1337)
      val todo         = Todo(Option("Title"), None, None, orderId)
      val posted       = postTodo(todo).sendSync.body
      val patchOrderId = Option(42)
      patchTodo(posted.id, Todo(None, None, None, patchOrderId)).sendSync
      getTodo(posted.id).sendSync.body.order shouldBe patchOrderId
    }
  }

  override protected def withFixture(test: OneArgTest): Outcome =
    serverResource.use(server => IO.pure(test(server))).unsafeRunSync()

  override type FixtureParam = Server[IO]
}
