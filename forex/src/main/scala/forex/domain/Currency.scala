package forex.domain

import cats.Show
import io.circe._

sealed trait Currency
object Currency {

  final case object EUR extends Currency
  final case object USD extends Currency
  final case object JPY extends Currency
  final case object CHF extends Currency
  final case object AUD extends Currency
  final case object CAD extends Currency
  final case object NZD extends Currency
  final case object GBP extends Currency
  final case object SEK extends Currency
  final case object NOK extends Currency
  final case object MXN extends Currency
  final case object TRY extends Currency
  final case object ZAR extends Currency
  final case object CNH extends Currency
  final case object XAU extends Currency
  final case object XAG extends Currency
  final case object SGD extends Currency
  final case object RUB extends Currency
  final case object HKD extends Currency
  final case object DKK extends Currency
  final case object PLN extends Currency
  final case object BTC extends Currency
  final case object ETH extends Currency
  final case object LTC extends Currency
  final case object XRP extends Currency
  final case object DSH extends Currency
  final case object BCH extends Currency

  val listOfCurrencies = List(
    EUR,
    USD,
    JPY,
    CHF,
    AUD,
    CAD,
    NZD,
    GBP,
    SEK,
    NOK,
    MXN,
    TRY,
    ZAR,
    CNH,
    XAU,
    XAG,
    SGD,
    RUB,
    HKD,
    DKK,
    PLN,
    BTC,
    ETH,
    LTC,
    XRP,
    DSH,
    BCH)

  implicit val show: Show[Currency] = Show.show(_.toString)

  def fromString(s: String): Either[String,Currency] = {
    val upperCaseString = s.toUpperCase
    listOfCurrencies
      .find(_.toString == upperCaseString)
      .toRight(s"$s is not a supported currency code")
  }

  implicit val encoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val decoder: Decoder[Currency] = Decoder.instance { cursor =>
    def currencyFromString(currencyCode: String): Decoder.Result[Currency] = fromString(currencyCode) match {
      case Right(currency) => Right(currency)
      case Left(error) => Left(DecodingFailure(error, Nil))
    }

     for {
      currencyCode <- cursor.as[String]
      currency <- currencyFromString(currencyCode)
    } yield currency
  }

}
