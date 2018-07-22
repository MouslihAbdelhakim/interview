package forex.services.oneforge

import akka.stream.Materializer
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.interfaces.api.rates.Converters
import forex.services.oneforge.OneForgeBackEnd.IncomingMessages.{
  InformationRetrievedFromOneForge,
  NoRatesRetrieved,
  RetrievedRates
}
import forex.services.oneforge.OneForgeBackEnd.LoggableError
import forex.services.oneforge.OneForgeHttpClientImplementation.{
  OneForgeJsonError,
  OneForgeListOfQuotes,
  OneForgeResponse
}

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }
import cats.implicits._

class OneForgeEndPoint(oneForgeHttpClient: OneForgeHttpClient) {

  def refresh(implicit executionContext: ExecutionContext,
              materializer: Materializer): Future[InformationRetrievedFromOneForge] = {
    val informationRetrievedFromOneForge = for {
      quotes ← oneForgeHttpClient.retrieve
    } yield {
      val (errors, rates) = getErrorsAndRates(quotes)
      constructInformationRetrievedFromOneForge(errors, rates)
    }

    informationRetrievedFromOneForge.fold(
      error ⇒ NoRatesRetrieved(List(LoggableError(error.message))),
      info ⇒ info
    )
  }

  private def constructInformationRetrievedFromOneForge(loggableErrors: List[LoggableError], rates: List[Rate]) =
    if (rates.isEmpty) {
      val noRatesRetrievedError = LoggableError("No rates retrieved from oneForge")
      NoRatesRetrieved(noRatesRetrievedError :: loggableErrors)
    } else {
      val availableRates = constructMapOfAvailableRates(rates)
      val oldestTimeStamp = getOldestTimeStamp(rates)
      RetrievedRates(availableRates, oldestTimeStamp, loggableErrors)
    }

  private def getErrorsAndRates(oneForgeResponse: OneForgeResponse): (List[LoggableError], List[Rate]) = {
    @tailrec def partitionToErrorsAndRate(maybeRate: List[Either[LoggableError, Rate]],
                                          errors: List[LoggableError],
                                          rates: List[Rate]): (List[LoggableError], List[Rate]) =
      maybeRate match {
        case Nil                 ⇒ (errors, rates)
        case Right(rate) :: rest ⇒ partitionToErrorsAndRate(rest, errors, rate :: rates)
        case Left(error) :: rest ⇒ partitionToErrorsAndRate(rest, error :: errors, rates)
      }

    oneForgeResponse match {
      case OneForgeJsonError(_, message) ⇒ (List(LoggableError(message)), Nil)
      case OneForgeListOfQuotes(oneForgeQuotes) ⇒
        val maybeRate = oneForgeQuotes.map(Converters.toRate(_))
        partitionToErrorsAndRate(maybeRate, Nil, Nil)
    }
  }

  private def constructMapOfAvailableRates(rates: List[Rate]): Map[Pair, Rate] =
    rates.map(rate ⇒ rate.pair → rate).toMap

  private def getOldestTimeStamp(rates: List[Rate]): Long =
    rates.map(_.timestamp.value.toEpochSecond).min

}
