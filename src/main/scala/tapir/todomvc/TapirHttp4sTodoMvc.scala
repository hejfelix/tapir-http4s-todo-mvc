package tapir.todomvc
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.server.middleware.CORSConfig
import tapir.json.circe._
import tapir.{endpoint, jsonBody, path, stringBody, Endpoint}

import scala.collection.mutable
import scala.concurrent.duration._

abstract class TapirHttp4sTodoMvc[F[_]: Sync]() {

  private val todos: mutable.Map[String, Todo] = mutable.Map[String, Todo]()

  val F: Sync[F] = implicitly[Sync[F]]

  def port: Int
  def hostName: String

  protected val corsConfig =
    CORSConfig(anyOrigin = true, anyMethod = true, allowCredentials = true, maxAge = 1.day.toSeconds)

  val postEndpoint: Endpoint[Todo, String, Todo] =
    endpoint.post
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)

  val deleteEndpoint: Endpoint[Unit, String, List[Todo]] =
    endpoint.delete
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)

  val getEndpoint: Endpoint[Unit, String, List[Todo]] =
    endpoint.get
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)

  val getTodoEndpoint: Endpoint[String, String, Todo] =
    endpoint.get
      .in(path[String]("uuid"))
      .out(jsonBody[Todo])
      .errorOut(stringBody)

  val patchByIdEndpoint: Endpoint[(String, Todo), String, Todo] =
    endpoint.patch
      .in(path[String]("uuid"))
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)

  def postTodo(todo: Todo): F[Either[String, Todo]] = {
    val uuid = java.util.UUID.randomUUID().toString
    val incompleteTodo =
      todo.copy(completed = Option(false), url = Option(s"http://$hostName:${port}/$uuid"))
    F.delay(
      println(todos.+=(uuid -> incompleteTodo))
    ) *> F.delay(Either.right(incompleteTodo))
  }

  def deleteTodo(): F[Either[String, List[Todo]]] =
    F.delay(
      println(todos.clear())
    ) *> F.delay(Either.right(List.empty[Todo]))

  def getTodoById(uuid: String): F[Either[String, Todo]] =
    F.delay(
      println(s"hashcode: ${uuid}, ${todos.keys}")
    ) *> F.delay(
      todos.get(uuid).toRight(s"$uuid not found")
    )

  def getTodo(): F[Either[String, List[Todo]]] = F.pure(Either.right(todos.values.toList))

  def patchById(uuid: String, todo: Todo): F[Either[String, Todo]] =
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
