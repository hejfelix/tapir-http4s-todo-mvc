package tapir.todomvc
import java.util.UUID

import io.circe.generic.auto._
import tapir.Codec.PlainCodec
import tapir._
import tapir.json.circe._
import cats.implicits._
import tapir.DecodeResult.{Missing, Value}
import tapir.docs.openapi._
import tapir.openapi.circe.yaml._
import tapir.openapi.{Info, OpenAPI}

class Endpoints(val basePath: String) {

  implicit private val uuidCodec: PlainCodec[UUID] =
    Codec.stringPlainCodecUtf8
      .mapDecode(
        s => Either.catchNonFatal(UUID.fromString(s)).fold(_ => Missing, Value(_))
      )(_.toString)

  private lazy val info = Info(
    "TodoMVC-Backend",
    "1.0",
  )

  private def allDocs: OpenAPI =
    List(
      deleteEndpoint.toOpenAPI(info),
      getEndpoint.toOpenAPI(info),
      getTodoEndpoint.toOpenAPI(info),
      patchByIdEndpoint.toOpenAPI(info),
      deleteTodoEndpoint.toOpenAPI(info)
    ).foldLeft(postEndpoint.toOpenAPI(info))((api, api2) =>
      api2.paths.foldLeft(api)((api, path) => api.addPathItem(path._1, path._2)))

  def openApiYaml: String = allDocs.toYaml

  lazy val postEndpoint: Endpoint[Todo, String, Todo, Nothing] =
    endpoint.post
      .in(basePath)
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)

  lazy val deleteTodoEndpoint: Endpoint[UUID, String, List[Todo], Nothing] =
    endpoint.delete
      .in(basePath / path[UUID]("uuid"))
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)

  lazy val deleteEndpoint: Endpoint[Unit, String, List[Todo], Nothing] =
    endpoint.delete
      .in(basePath)
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)

  lazy val getEndpoint: Endpoint[Unit, String, List[Todo], Nothing] =
    endpoint.get
      .in(basePath)
      .out(jsonBody[List[Todo]])
      .errorOut(stringBody)

  lazy val getTodoEndpoint: Endpoint[UUID, String, Todo, Nothing] =
    endpoint.get
      .in(basePath / path[UUID]("uuid"))
      .out(jsonBody[Todo])
      .errorOut(stringBody)

  lazy val patchByIdEndpoint: Endpoint[(UUID, Todo), String, Todo, Nothing] =
    endpoint.patch
      .in(basePath / path[UUID]("uuid")(uuidCodec))
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)

}
