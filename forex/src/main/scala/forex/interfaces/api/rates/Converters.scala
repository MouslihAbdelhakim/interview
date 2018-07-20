package forex.interfaces.api.rates

import forex.domain._
import forex.interfaces.api.utils.Error.ApiError
import forex.processes.rates.messages._

object Converters {
  import Protocol._

  def toGetRequest(
      request: GetApiRequest
  ): Either[ApiError,GetRequest] = request match {
    case GetApiRequest(Right(from), Right(to)) ⇒
      Right(GetRequest(from, to))

    case GetApiRequest(Left(fromErrorMessage), Left(toErrorMessage)) ⇒
      Left(ApiError(s"$fromErrorMessage and $toErrorMessage"))

    case GetApiRequest(Left(errorMessage),_) ⇒
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

}
