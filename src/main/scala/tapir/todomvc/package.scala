package tapir
import cats.FlatMap
import cats.implicits._
import tapir.DecodeResult.{Missing, Value}
import tapir.GeneralCodec.PlainCodec
import tapir.MediaType.TextPlain

package object todomvc {

  implicit val flatMapDecodeResult: FlatMap[DecodeResult] = new FlatMap[DecodeResult] {
    override def flatMap[A, B](fa: DecodeResult[A])(f: A => DecodeResult[B]): DecodeResult[B] = fa match {
      case Missing               => Missing
      case e: DecodeResult.Error => e
      case Value(v)              => f(v)
    }

    override def tailRecM[A, B](a: A)(f: A => DecodeResult[Either[A, B]]): DecodeResult[B] = ???
    override def map[A, B](fa: DecodeResult[A])(f: A => B): DecodeResult[B]                = fa.map(f)
  }

  implicit class MyCodec[T](plainCodec: PlainCodec[T]) {
    def plainMap[TT](f: T => DecodeResult[TT])(g: TT => T): PlainCodec[TT] =
      new PlainCodec[TT] {
        override def encode(t: TT): String               = plainCodec.encode(g(t))
        override def decode(s: String): DecodeResult[TT] = plainCodec.decode(s).flatMap(f)
        override val rawValueType: RawValueType[String]  = plainCodec.rawValueType
        override def schema: Schema                      = plainCodec.schema
        override def mediaType: TextPlain                = plainCodec.mediaType
      }
  }

}
