package todomvc

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import com.comcast.ip4s.{Hostname, Port}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.util.UUID
import scala.collection.mutable

class Implementation[F[_]: Async](port: Port, hostName: Hostname, endpoints: Endpoints)(implicit
    F: Sync[F],
) {

  object dsl extends Http4sDsl[F]
  import dsl.*
//  import endpoints.*

  private val interpreter = Http4sServerInterpreter[F]()

  def routes: HttpRoutes[F] = {
    import sttp.tapir.*
    val value: List[ServerEndpoint[Any, F]] =
      List(
        endpoints.getEndpoint.serverLogic(_ => getTodo),
        endpoints.getTodoEndpoint.serverLogic(getTodoById),
        endpoints.postEndpoint.serverLogic(postTodo),
        endpoints.deleteTodoEndpoint.serverLogic(deleteTodo),
        endpoints.deleteEndpoint.serverLogic(_ => deleteTodos),
        endpoints.patchByIdEndpoint.serverLogic(patchById)
      )
    interpreter.toRoutes(value ++ endpoints.openApiYaml[F])
  }

  private val todos: mutable.Map[UUID, Todo] = mutable.Map[UUID, Todo]()

  private def postTodo(todo: Todo): F[Either[String, Todo]] = {
    val uuid = java.util.UUID.randomUUID()
    val incompleteTodo =
      todo.copy(
        completed = Option(false),
        url = Option(s"http://$hostName:${port}/${endpoints.basePath}/$uuid"),
      )
    F.delay {
      println(s"Posting TODOS: $incompleteTodo")
      println(todos.+=(uuid -> incompleteTodo))
    } *> F.delay(Either.right(incompleteTodo))
  }

  private def deleteTodos: F[Either[String, List[Todo]]] =
    F.delay(
      println(todos.clear()),
    ) *> F.delay(Either.right(List.empty[Todo]))

  private def deleteTodo(uuid: UUID): F[Either[String, List[Todo]]] =
    F.delay(
      println(todos.remove(uuid)),
    ) *> F.delay(Either.right(List.empty[Todo]))

  private def getTodoById(uuid: UUID): F[Either[String, Todo]] =
    F.delay(
      println(s"hashcode: $uuid, ${todos.keys}"),
    ) *> F.delay(
      todos.get(uuid).toRight(s"$uuid not found"),
    )

  private def getTodo: F[Either[String, List[Todo]]] =
    F.delay {
      println(s"Getting ALL TODOS!")
    } *> F.pure(Either.right(todos.values.toList))

  private def patchById(uuid: UUID, todo: Todo): F[Either[String, Todo]] =
    F.pure(
      println(s"PATCH: ${uuid}, $todo}"),
    ) *> F.delay {
      todos
        .get(uuid)
        .map(_.patch(todo))
        .flatMap(patched => todos.put(uuid, patched))
        .flatMap(_ => todos.get(uuid))
        .toRight(s"$uuid not found")
    }

}
