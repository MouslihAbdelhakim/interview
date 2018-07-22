package forex.interfaces.api.rates

import forex.domain._
import forex.interfaces.api.utils.Error.ApiError
import forex.processes.rates.messages._
import forex.services.oneforge.OneForgeHttpClientImplementation.OneForgeQuote
import Rate.Pair
import forex.services.oneforge.OneForgeBackEnd.LoggableError

object Converters {
  import Protocol._

  def toGetRequest(
      request: GetApiRequest
  ): Either[ApiError, GetRequest] = request match {
    case GetApiRequest(Right(from), Right(to)) ⇒
      Right(GetRequest(from, to))

    case GetApiRequest(Left(fromErrorMessage), Left(toErrorMessage)) ⇒
      Left(ApiError(s"$fromErrorMessage and $toErrorMessage"))

    case GetApiRequest(Left(errorMessage), _) ⇒
      Left(ApiError(errorMessage))

    case GetApiRequest(_, Left(errorMessage)) ⇒
      Left(ApiError(errorMessage))
  }

  def toGetApiResponse(
      rate: Rate
  ): GetApiResponse =
    GetApiResponse(
      from = rate.pair.from,
      to = rate.pair.to,
      price = rate.price,
      timestamp = rate.timestamp
    )

  def toRate(oneForgeQuote: OneForgeQuote): Either[LoggableError, Rate] = {

    def safeSubString(currencyPair: String, subStringFunction: String ⇒ String): Either[String, String] =
      if (currencyPair.length == 6)
        Right(subStringFunction(currencyPair))
      else
        Left(s"received an unusual currency pair: $currencyPair")

    val maybeRate = for {
      baseCurrencyCode ← safeSubString(oneForgeQuote.symbol, _.take(3))
      baseCurrency ← Currency.fromString(baseCurrencyCode)
      counterCurrencyCode ← safeSubString(oneForgeQuote.symbol, _.drop(3))
      counterCurrency ← Currency.fromString(counterCurrencyCode)
    } yield Rate(Pair(counterCurrency, baseCurrency), Price(oneForgeQuote.bid), Timestamp(oneForgeQuote.timestamp))

    maybeRate.left.map(LoggableError(_))
  }

}
