package tapir.todomvc
import java.util.UUID

import cats.effect._
import cats.implicits._
import cats.mtl.FunctorRaise
import cats.mtl.implicits._
import org.http4s.dsl.Http4sDsl

class Implementation[F[_]: ContextShift: TodoStore: Log: IdGen: FunctorRaise[*[_], String]](
    port: Int,
    hostName: String,
    endpoints: Endpoints)(implicit F: Sync[F]) {

  object dsl extends Http4sDsl[F]

  def postTodo(todo: Todo): F[Todo] =
    for {
      id <- IdGen[F].newId
      incompleteTodo = todo.copy(completed = Option(false),
                                 url = Option(s"http://$hostName:${port}/${endpoints.basePath}/$id"))
      _ <- Log[F].info(s"Posting TODO: $incompleteTodo")
      _ <- TodoStore[F].put(id, incompleteTodo)
    } yield incompleteTodo

  def deleteTodos: F[List[Todo]] =
    for {
      _ <- Log[F].info("Deleting todos...")
      _ <- TodoStore[F].truncate()
    } yield List.empty

  def deleteTodo(uuid: UUID): F[List[Todo]] =
    for {
      _ <- Log[F].info(s"Deleting todo with id: $uuid")
      _ <- TodoStore[F].delete(uuid)
    } yield List.empty

  def getTodoById(uuid: UUID): F[Todo] =
    for {
      _         <- Log[F].info(s"Getting todo, id: $uuid")
      maybeTodo <- TodoStore[F].get(uuid)
      todo      <- maybeTodo.fold(s"$uuid not found".raise[F, Todo])(_.pure[F])
    } yield todo

  def getTodo: F[List[Todo]] =
    for {
      _     <- Log[F].info(s"Getting ALL TODOS!")
      todos <- TodoStore[F].getAll
    } yield todos

  def patchById(uuid: UUID, patchTodo: Todo): F[Todo] =
    for {
      _         <- Log[F].info(s"Patching: $uuid, $patchTodo")
      maybeTodo <- TodoStore[F].get(uuid)
      todo      <- maybeTodo.fold(s"No patch with id: $uuid".raise[F, Todo])(_.pure[F])
      patched = todo.patch(patchTodo)
      _ <- TodoStore[F].update(uuid, patched)
    } yield patched

}
