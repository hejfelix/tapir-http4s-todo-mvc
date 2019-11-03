package tapir.todomvc

import cats.effect.Sync

object Log {
  def apply[F[_]: Log]: Log[F] = implicitly[Log[F]]

  implicit def default[F[_]: Sync]: Log[F] = msg => Sync[F].delay(println(msg))
}
trait Log[F[_]] {
  def info(message: String): F[Unit]
}
