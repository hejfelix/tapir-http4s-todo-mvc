package tapir.todomvc
import java.util.UUID

import cats.effect._
import cats.implicits._
import cats.mtl.{ApplicativeAsk, FunctorRaise}
import cats.mtl.implicits._

object Implementation {

  def postTodo[F[_]: Sync: TodoStore: IdGen: ApplicativeAsk[*[_], TodoConfig]](todo: Todo): F[Todo] =
    for {
      id     <- IdGen[F].newId
      config <- ApplicativeAsk[F, TodoConfig].ask
      incompleteTodo = todo.copy(
        completed = Option(false),
        url = Option(s"http://${config.host}:${config.port}/${config.basePath}/$id"),
      )
      _ <- Log[F].info(s"Posting TODO: $incompleteTodo")
      _ <- TodoStore[F].put(id, incompleteTodo)
    } yield incompleteTodo

  def deleteTodos[F[_]: Log: Sync: TodoStore]: F[List[Todo]] =
    for {
      _ <- Log[F].info("Deleting todos...")
      _ <- TodoStore[F].truncate()
    } yield List.empty

  def deleteTodo[F[_]: Sync: Log: TodoStore](uuid: UUID): F[List[Todo]] =
    for {
      _ <- Log[F].info(s"Deleting todo with id: $uuid")
      _ <- TodoStore[F].delete(uuid)
    } yield List.empty

  def getTodoById[F[_]: Sync: Log: TodoStore: FunctorRaise[*[_], String]](uuid: UUID): F[Todo] =
    for {
      _         <- Log[F].info(s"Getting todo, id: $uuid")
      maybeTodo <- TodoStore[F].get(uuid)
      todo      <- maybeTodo.fold(s"$uuid not found".raise[F, Todo])(_.pure[F])
    } yield todo

  def getTodo[F[_]: Sync: Log: TodoStore]: F[List[Todo]] =
    for {
      _     <- Log[F].info(s"Getting ALL TODOS!")
      todos <- TodoStore[F].getAll
    } yield todos

  def patchById[F[_]: Sync: Log: TodoStore: FunctorRaise[*[_], String]](uuid: UUID, patchTodo: Todo): F[Todo] =
    for {
      _         <- Log[F].info(s"Patching: $uuid, $patchTodo")
      maybeTodo <- TodoStore[F].get(uuid)
      todo      <- maybeTodo.fold(s"No patch with id: $uuid".raise[F, Todo])(_.pure[F])
      patched = todo.patch(patchTodo)
      _ <- TodoStore[F].update(uuid, patched)
    } yield patched

}
