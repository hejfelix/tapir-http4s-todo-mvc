package tapir.todomvc

import java.util.UUID

import cats.effect.Sync

object IdGen {
  def apply[F[_]: IdGen]: IdGen[F] = implicitly[IdGen[F]]
  def default[F[_]: Sync]: IdGen[F] = new IdGen[F] {
    override def newId: F[UUID] = Sync[F].delay(UUID.randomUUID())
  }
}
trait IdGen[F[_]] {
  def newId: F[UUID]
}
