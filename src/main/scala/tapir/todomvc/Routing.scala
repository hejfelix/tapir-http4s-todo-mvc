package tapir.todomvc

import cats.data.NonEmptyList
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import cats.mtl.{ApplicativeAsk, ApplicativeHandle, FunctorRaise}
import cats.~>
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.{HttpRoutes, Uri}
import tapir.server.http4s.RichHttp4sHttpEndpoint
import cats.mtl.implicits._

object Routing {

  def docsRoute[F[_]: Sync](dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "docs" =>
        PermanentRedirect(Location(Uri.uri("/swagger-ui/3.20.9/index.html?url=/openapi.yml#/")))
    }
  }

  def openApiRoute[F[_]: Sync](openApiYaml: String, dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "openapi.yml" => Ok(openApiYaml)
    }
  }

  def route[F[_]: Sync: ApplicativeHandle[*[_], String]: FunctorRaise[*[_], String]: TodoStore: IdGen: Log: ApplicativeAsk[
    *[_],
    TodoConfig,
  ], G[_]: Sync: ContextShift](
      endpoints: Endpoints,
      fToG: F ~> G,
  ): HttpRoutes[G] = {
    import Implementation._
    import endpoints._

    def massage[T, U](f: T => F[U]): T => G[Either[String, U]] =
      f.andThen(_.attemptHandle).andThen(fToG(_))

    val http4sDslG = new Http4sDsl[G] {}

    NonEmptyList
      .of(
        getEndpoint.toRoutes(logic = _ => fToG(getTodo[F].attemptHandle)),
        deleteTodoEndpoint.toRoutes(logic = massage(deleteTodo[F])),
        deleteEndpoint.toRoutes(logic = _ => fToG(deleteTodos[F].attemptHandle)),
        postEndpoint.toRoutes(logic = massage(postTodo[F])),
        getTodoEndpoint.toRoutes(logic = massage(getTodoById[F])),
        patchByIdEndpoint.toRoutes(logic = massage((patchById[F] _).tupled)),
        docsRoute[G](http4sDslG),
        openApiRoute[G](openApiYaml, http4sDslG),
      )
      .reduceK
  }

}
