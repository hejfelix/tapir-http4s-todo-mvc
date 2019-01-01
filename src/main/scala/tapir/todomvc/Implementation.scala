package tapir.todomvc
import java.util.UUID

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import tapir.server.http4s._

import scala.collection.mutable

class Implementation[F[_]](port: Int, hostName: String, endpoints: Endpoints)(implicit F: Sync[F]) {

  import endpoints._

  def routes: HttpRoutes[F] =
    NonEmptyList
      .of(
        getEndpoint.toHttp4sRoutes(getTodo _),
        deleteEndpoint.toHttp4sRoutes(deleteTodo _),
        postEndpoint.toHttp4sRoutes(postTodo _),
        getTodoEndpoint.toHttp4sRoutes(getTodoById _),
        patchByIdEndpoint.toHttp4sRoutes(patchById _)
      )
      .reverse // There's a bug in the tapir-http4s backend that doesnt fail when all path is not consumed
      .reduceLeft(_ <+> _)

  private val todos: mutable.Map[UUID, Todo] = mutable.Map[UUID, Todo]()

  private def postTodo(todo: Todo): F[Either[String, Todo]] = {
    val uuid = java.util.UUID.randomUUID()
    val incompleteTodo =
      todo.copy(completed = Option(false), url = Option(s"http://$hostName:${port}/$uuid"))
    F.delay(
      println(todos.+=(uuid -> incompleteTodo))
    ) *> F.delay(Either.right(incompleteTodo))
  }

  private def deleteTodo(): F[Either[String, List[Todo]]] =
    F.delay(
      println(todos.clear())
    ) *> F.delay(Either.right(List.empty[Todo]))

  private def getTodoById(uuid: UUID): F[Either[String, Todo]] =
    F.delay(
      println(s"hashcode: ${uuid}, ${todos.keys}")
    ) *> F.delay(
      todos.get(uuid).toRight(s"$uuid not found")
    )

  private def getTodo(): F[Either[String, List[Todo]]] = F.pure(Either.right(todos.values.toList))

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
