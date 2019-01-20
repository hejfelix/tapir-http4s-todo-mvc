package tapir.todomvc
import java.util.UUID

import io.circe.generic.auto._
import tapir.GeneralCodec.PlainCodec
import tapir._
import tapir.json.circe._
import cats.implicits._
import tapir.DecodeResult.{Missing, Value}

class Endpoints {

  implicit private val uuidCodec: PlainCodec[UUID] =
    GeneralCodec.stringPlainCodecUtf8
      .mapDecode(
        s => Either.catchNonFatal(UUID.fromString(s)).fold(_ => Missing, Value(_))
      )(_.toString)

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

  val getTodoEndpoint: Endpoint[UUID, String, Todo] =
    endpoint.get
      .in(path[UUID]("uuid"))
      .out(jsonBody[Todo])
      .errorOut(stringBody)

  val patchByIdEndpoint: Endpoint[(UUID, Todo), String, Todo] =
    endpoint.patch
      .in(path[UUID]("uuid")(uuidCodec))
      .in(jsonBody[Todo])
      .out(jsonBody[Todo])
      .errorOut(stringBody)

}
