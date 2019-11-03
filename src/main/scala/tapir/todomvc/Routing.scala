package tapir.todomvc

import java.util.UUID

import cats.{~>, ApplicativeError}
import cats.data.NonEmptyList
import cats.effect.{ContextShift, Sync}
import cats.mtl.FunctorRaise
import org.http4s.{HttpRoutes, HttpService, Uri}
import tapir.server.http4s.RichHttp4sHttpEndpoint
import cats.syntax.reducible.catsSyntaxNestedReducible
import cats.syntax.applicativeError._
import cats.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
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

  def route[F[_]: ApplicativeError[*[_], String], G[_]: Sync: ContextShift](endpoints: Endpoints,
                                                                            implementation: Implementation[F],
                                                                            fToG: F ~> G): HttpRoutes[G] = {
    import endpoints._
    import implementation._

    def massage[T, U](f: T => F[U]): T => G[Either[String, U]] = f.andThen(_.attempt).andThen(fToG(_))

    val http4sDslG = new Http4sDsl[G] {}

    NonEmptyList
      .of(
        getEndpoint.toRoutes(logic = _ => fToG(getTodo.attempt)),
        deleteTodoEndpoint.toRoutes(logic = massage(deleteTodo)),
        deleteEndpoint.toRoutes(logic = _ => fToG(deleteTodos.attempt)),
        postEndpoint.toRoutes(logic = massage(postTodo)),
        getTodoEndpoint.toRoutes(logic = massage(getTodoById)),
        patchByIdEndpoint.toRoutes(logic = massage((patchById _).tupled)),
        docsRoute[G](http4sDslG),
        openApiRoute[G](openApiYaml, http4sDslG),
      )
      .reduceK
  }

}
