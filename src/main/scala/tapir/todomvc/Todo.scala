package tapir.todomvc
import cats.implicits._

case class Todo(title: Option[String], completed: Option[Boolean], url: Option[String], order: Option[Int]) {
  def patch(that: Todo) =
    Todo(that.title, that.completed <+> completed, that.url <+> url, that.order <+> order)
}
