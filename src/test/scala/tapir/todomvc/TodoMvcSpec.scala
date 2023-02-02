package tapir.todomvc
import java.util.UUID
import cats.effect.*
import org.http4s.HttpRoutes
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig}
import org.http4s.syntax.kleisli.http4sKleisliResponseSyntax
import org.scalatest.{Matchers, Outcome}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import tapir.client.sttp.*
import com.softwaremill.sttp.*
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import tapir.Endpoint
import tapir.*
import _root_.todomvc.Todo
import todomvc.{Endpoints, Implementation}

class TodoMvcSpec extends org.scalatest.fixture.WordSpec with Matchers {

  private val baseUri = uri"http://127.0.0.1:8080"
  private val port    = 8080

  private val executionContext: ExecutionContext      = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(executionContext)
  private implicit val timer                          = IO.timer(executionContext)

  private implicit val catsSttpBackend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[IO]()

  private val endpoints      = new Endpoints("basepath")
  private val implementation = new Implementation[IO](port, baseUri.host, endpoints)

  private val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  private val combinedRoutes: HttpRoutes[IO] = implementation.routes
  private val routes                         = CORS(combinedRoutes, corsConfig)

  private def serverResource: Resource[IO, Server[IO]] =
    BlazeServerBuilder[IO]
      .bindHttp(port, baseUri.host)
      .withHttpApp(routes.orNotFound)
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

  val getTodos = endpoints.getEndpoint.toSttpRequest(baseUri).apply()
  val delete   = endpoints.deleteEndpoint.toSttpRequest(baseUri).apply()

  def getTodo(id: UUID): Request[Either[String, Todo], Nothing] = {
    val endpoint: Endpoint[UUID, String, Todo, Nothing] = endpoints.getTodoEndpoint
    endpoint.toSttpRequest(baseUri).apply(id)
  }

  def postTodo(todo: Todo): Request[Either[String, Todo], Nothing] =
    endpoints.postEndpoint
      .toSttpRequest(baseUri)
      .apply(todo)

  def patchTodo(id: UUID, patch: Todo): Request[Either[String, Todo], Nothing] =
    endpoints.patchByIdEndpoint.toSttpRequest(baseUri).apply(id, patch)

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
