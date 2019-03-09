package tapir.todomvc
import java.util.UUID

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import org.http4s.HttpRoutes
import tapir.server.http4s._
import org.http4s.dsl.Http4sDsl

import scala.collection.mutable
import org.http4s._
import org.http4s.headers.Location

class Implementation[F[_]: ContextShift](port: Int, hostName: String, endpoints: Endpoints)(implicit F: Sync[F]) {

  object dsl extends Http4sDsl[F]
  import dsl._
  import endpoints._

  def routes: HttpRoutes[F] =
    NonEmptyList
      .of(
        getEndpoint.toRoutes(logic = getTodo _),
        deleteTodoEndpoint.toRoutes(logic = deleteTodo _),
        deleteEndpoint.toRoutes(logic = deleteTodos _),
        postEndpoint.toRoutes(logic = postTodo _),
        getTodoEndpoint.toRoutes(logic = getTodoById _),
        patchByIdEndpoint.toRoutes(logic = patchById _),
        openApiRoute,
        docsRoute
      )
      .reduceK

  private val todos: mutable.Map[UUID, Todo] = mutable.Map[UUID, Todo]()

  private def docsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "docs" =>
      PermanentRedirect(Location(Uri.uri("/swagger-ui/3.20.9/index.html?url=/openapi.yml#/")))
  }

  private def openApiRoute: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "openapi.yml" => Ok(openApiYaml)
    }

  private def postTodo(todo: Todo): F[Either[String, Todo]] = {
    val uuid = java.util.UUID.randomUUID()
    val incompleteTodo =
      todo.copy(completed = Option(false), url = Option(s"http://$hostName:${port}/${endpoints.basePath}/$uuid"))
    F.delay {
      println(s"Posting TODOS: $incompleteTodo")
      println(todos.+=(uuid -> incompleteTodo))
    } *> F.delay(Either.right(incompleteTodo))
  }

  private def deleteTodos: F[Either[String, List[Todo]]] =
    F.delay(
      println(todos.clear())
    ) *> F.delay(Either.right(List.empty[Todo]))

  private def deleteTodo(uuid: UUID): F[Either[String, List[Todo]]] =
    F.delay(
      println(todos.remove(uuid))
    ) *> F.delay(Either.right(List.empty[Todo]))

  private def getTodoById(uuid: UUID): F[Either[String, Todo]] =
    F.delay(
      println(s"hashcode: $uuid, ${todos.keys}")
    ) *> F.delay(
      todos.get(uuid).toRight(s"$uuid not found")
    )

  private def getTodo(): F[Either[String, List[Todo]]] =
    F.delay {
      println(s"Getting ALL TODOS!")
    } *> F.pure(Either.right(todos.values.toList))

  private def patchById(uuid: UUID, todo: Todo): F[Either[String, Todo]] =
    F.pure(
      println(s"PATCH: ${uuid}, $todo}")
    ) *> F.delay {
      todos
        .get(uuid)
        .map(_.patch(todo))
        .flatMap(patched => todos.put(uuid, patched))
        .flatMap(_ => todos.get(uuid))
        .toRight(s"$uuid not found")
    }

}
