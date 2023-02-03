package todomvc

import cats.effect.*
import cats.effect.std.{Console, UUIDGen}
import cats.implicits.*
import com.comcast.ip4s.{Hostname, Port}
import org.http4s.HttpRoutes
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.util.UUID

class Implementation[F[_]: Async: Console: UUIDGen](
    port: Port,
    hostName: Hostname,
    endpoints: Endpoints,
    todos: Ref[F, Map[UUID, Todo]],
):

  private val interpreter = Http4sServerInterpreter[F]()

  def routes: HttpRoutes[F] =
    val value: List[ServerEndpoint[Any, F]] =
      List(
        endpoints.getEndpoint.serverLogic(_ => getTodo),
        endpoints.getTodoEndpoint.serverLogic(getTodoById),
        endpoints.postEndpoint.serverLogic(postTodo),
        endpoints.deleteTodoEndpoint.serverLogic(deleteTodo),
        endpoints.deleteEndpoint.serverLogic(_ => deleteTodos),
        endpoints.patchByIdEndpoint.serverLogic(patchById),
      )
    interpreter.toRoutes(value ++ endpoints.openApiYaml[F])

  private def postTodo(todo: Todo): F[Either[String, Todo]] =
    for
      uuid <- UUIDGen[F].randomUUID
      incompleteTodo =
        todo.copy(
          completed = Option(false),
          url = Option(s"http://$hostName:${port}/${endpoints.basePath}/$uuid"),
        )
      _ <- Console[F].println(s"Posting TODOS: $incompleteTodo")
      _ <- todos.update(_ + (uuid -> incompleteTodo))
    yield incompleteTodo.asRight

  private def deleteTodos: F[Either[String, List[Todo]]] =
    Console[F].println("Deleting todos") >> todos.set(Map.empty).as(List.empty.asRight)

  private def deleteTodo(uuid: UUID): F[Either[String, List[Todo]]] =
    Console[F]
      .println(s"Deleting $uuid") >> todos.update(_.removed(uuid)).as(List.empty[Todo].asRight)

  private def getTodoById(uuid: UUID): F[Either[String, Todo]] =
    Console[F].println(s"Getting $uuid") >> todos.get.map(_.get(uuid).toRight(s"$uuid not found"))

  private def getTodo: F[Either[String, List[Todo]]] =
    Console[F].println("Getting all todos") >> todos.get.map(_.values.toList.asRight)

  private def patchById(uuid: UUID, todo: Todo): F[Either[String, Todo]] =
    for
      _            <- Console[F].println(s"Patching $uuid")
      todosMap     <- todos.get
      maybePatched <- todosMap.get(uuid).map(_.patch(todo)).pure
      _            <- maybePatched.traverse(patched => todos.update(_.updated(uuid, patched)))
    yield maybePatched.toRight(s"$uuid not found")

end Implementation
