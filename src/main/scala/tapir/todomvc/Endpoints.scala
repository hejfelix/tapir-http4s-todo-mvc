package tapir.todomvc
import java.util.UUID

import cats.implicits._
import io.circe.generic.auto._
import tapir.Codec.PlainCodec
import tapir.DecodeResult.{Missing, Value}
import tapir._
import tapir.docs.openapi._
import tapir.json.circe._
import tapir.openapi.Info
import tapir.openapi.circe.yaml._

class Endpoints(val basePath: String) {

  implicit private val uuidCodec: PlainCodec[UUID] =
    Codec.stringPlainCodecUtf8
      .mapDecode(
        s => Either.catchNonFatal(UUID.fromString(s)).fold(_ => Missing, Value(_))
      )(_.toString)

  private lazy val info = Info(
    "TodoMVC-Backend using Tapir",
    "1.0",
  )

  def openApiYaml: String =
    List(
      deleteEndpoint,
      getEndpoint,
      getTodoEndpoint,
      patchByIdEndpoint,
      deleteTodoEndpoint,
      postEndpoint
    ).toOpenAPI(info).toYaml

  lazy val postEndpoint: Endpoint[Todo, String, Todo, Nothing] =
    endpoint.post
      .in(basePath)
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)
      .description("Create a new TODO item")

  lazy val deleteTodoEndpoint: Endpoint[UUID, String, List[Todo], Nothing] =
    endpoint.delete
      .in(basePath / path[UUID]("uuid"))
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)
      .description("Delete a TODO item using its UUID")

  lazy val deleteEndpoint: Endpoint[Unit, String, List[Todo], Nothing] =
    endpoint.delete
      .in(basePath)
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)
      .description("Delete all todos")

  lazy val getEndpoint: Endpoint[Unit, String, List[Todo], Nothing] =
    endpoint.get
      .in(basePath)
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)
      .description("Retrieve all TODOs")

  lazy val getTodoEndpoint: Endpoint[UUID, String, Todo, Nothing] =
    endpoint.get
      .in(basePath / path[UUID]("uuid"))
      .out(jsonBody[Todo])
      .errorOut(stringBody)
      .description("Get TODO item by UUID")

  lazy val patchByIdEndpoint: Endpoint[(UUID, Todo), String, Todo, Nothing] =
    endpoint.patch
      .in(basePath / path[UUID]("uuid")(uuidCodec))
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)
      .description("Patch TODO item by UUID")

}
