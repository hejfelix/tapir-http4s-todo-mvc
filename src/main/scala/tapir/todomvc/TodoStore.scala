package tapir.todomvc

import java.util.UUID

import cats.Functor
import cats.effect.concurrent.Ref
import cats.implicits._

object TodoStore {
  def apply[F[_]: TodoStore]: TodoStore[F] = implicitly[TodoStore[F]]

  implicit def todoStore[F[_]](storeRef: Ref[F, Map[UUID, Todo]])(implicit F: Functor[F]): TodoStore[F] =
    new TodoStore[F] {
      override def put(id: UUID, todo: Todo): F[UUID]      = storeRef.modify(store => (store + (id -> todo), id))
      override def update(uuid: UUID, todo: Todo): F[UUID] = put(uuid, todo)
      override def get(uuid: UUID): F[Option[Todo]]        = storeRef.get.map(_.get(uuid))
      override def getAll: F[List[Todo]]                   = storeRef.get.map(_.values.toList)
      override def delete(uuid: UUID): F[Unit]             = storeRef.update(_ - uuid)
      override def truncate(): F[Unit]                     = storeRef.update(_ => Map.empty)
    }

}
trait TodoStore[F[_]] {
  def put(id: UUID, todo: Todo): F[UUID]
  def update(uuid: UUID, todo: Todo): F[UUID]
  def get(uuid: UUID): F[Option[Todo]]
  def getAll: F[List[Todo]]
  def delete(uuid: UUID): F[Unit]
  def truncate(): F[Unit]
}
