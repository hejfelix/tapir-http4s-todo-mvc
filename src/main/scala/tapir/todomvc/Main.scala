package tapir.todomvc
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import tapir.server.http4s._
import org.http4s.syntax.kleisli._

case class Todo(title: Option[String], completed: Option[Boolean], url: Option[String], order: Option[Int]) {
  def patch(that: Todo) =
    Todo(that.title, that.completed <+> completed, that.url <+> url, that.order <+> order)
}

object Main extends TapirHttp4sTodoMvc[IO] with IOApp {

  val hostName = "127.0.0.1"
  val port     = 8080

  override def run(
      args: List[String]
  ): IO[ExitCode] = {

    val _ = args

    val getRoute: HttpRoutes[IO]    = getEndpoint.toHttp4sRoutes(getTodo _)
    val deleteRoute: HttpRoutes[IO] = deleteEndpoint.toHttp4sRoutes(deleteTodo _)
    val postRoute: HttpRoutes[IO]   = postEndpoint.toHttp4sRoutes(postTodo _)
    val getById: HttpRoutes[IO]     = getTodoEndpoint.toHttp4sRoutes(getTodoById _)
    val patch: HttpRoutes[IO]       = patchByIdEndpoint.toHttp4sRoutes(patchById _)

    val combinedRoutes: HttpRoutes[IO] = getById <+> postRoute <+> getRoute <+> deleteRoute <+> patch

    val routes = CORS(combinedRoutes, corsConfig)

    BlazeServerBuilder[IO]
      .bindHttp(port, hostName)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .lastOrError
  }
}
