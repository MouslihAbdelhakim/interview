package forex.domain

import java.time.OffsetDateTime

import io.circe._
import io.circe.generic.semiauto._

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
) {
  override def equals(obj: scala.Any): Boolean = obj match {
    case that: Rate ⇒
      that.pair == this.pair &&
      that.price == this.price &&
      that.timestamp.value.toEpochSecond == this.timestamp.value.toEpochSecond

    case _ ⇒ false
  }
}

object Rate {

  def apply(
      from: Currency,
      to: Currency,
      price: Int,
      timestamp: OffsetDateTime
  ): Rate = Rate(
    Pair(from, to),
    Price(price),
    Timestamp(timestamp)
  )

  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    implicit val encoder: Encoder[Pair] =
      deriveEncoder[Pair]
  }

  implicit val encoder: Encoder[Rate] =
    deriveEncoder[Rate]
}
