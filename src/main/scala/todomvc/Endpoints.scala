package todomvc

import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.EndpointIO.Info
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.util.UUID

class Endpoints(val basePath: String) {

  def openApiYaml[F[_]]: List[ServerEndpoint[Any, F]] =
    SwaggerInterpreter().fromEndpoints[F](
      endpoints = List(
        deleteEndpoint,
        getEndpoint,
        getTodoEndpoint,
        patchByIdEndpoint,
        deleteTodoEndpoint,
        postEndpoint,
      ),
      title = "TodoMVC Tapir",
      version = "1.0",
    )

  lazy val postEndpoint: PublicEndpoint[Todo, String, Todo, Any] =
    endpoint.post
      .in(basePath)
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)
      .description("Create a new TODO item")

  lazy val deleteTodoEndpoint: PublicEndpoint[UUID, String, List[Todo], Any] =
    endpoint.delete
      .in(basePath / path[UUID]("uuid"))
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)
      .description("Delete a TODO item using its UUID")

  lazy val deleteEndpoint: PublicEndpoint[Unit, String, List[Todo], Any] =
    endpoint.delete
      .in(basePath)
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)
      .description("Delete all todos")

  lazy val getEndpoint: PublicEndpoint[Unit, String, List[Todo], Any] =
    endpoint.get
      .in(basePath)
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)
      .description("Retrieve all TODOs")

  lazy val getTodoEndpoint: PublicEndpoint[UUID, String, Todo, Any] =
    endpoint.get
      .in(basePath / path[UUID]("uuid"))
      .out(jsonBody[Todo])
      .errorOut(stringBody)
      .description("Get TODO item by UUID")

  lazy val patchByIdEndpoint: PublicEndpoint[(UUID, Todo), String, Todo, Any] =
    endpoint.patch
      .in(basePath / path[UUID]("uuid"))
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)
      .description("Patch TODO item by UUID")

}
