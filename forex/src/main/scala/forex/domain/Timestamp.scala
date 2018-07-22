package forex.domain

import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.java8.time._
import java.time.{ Instant, OffsetDateTime, ZoneOffset }

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(Instant.now().atOffset(ZoneOffset.UTC))

  def apply(epochInSeconds: Long): Timestamp =
    Timestamp(Instant.ofEpochSecond(epochInSeconds).atOffset(ZoneOffset.UTC))

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]

  implicit val decoder: Decoder[Timestamp] =
    deriveUnwrappedDecoder[Timestamp]
}
