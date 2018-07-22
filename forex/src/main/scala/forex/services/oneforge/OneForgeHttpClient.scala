package forex.services.oneforge

import akka.stream.Materializer
import akka.http.scaladsl.model.Uri
import cats.data.EitherT
import forex.services.oneforge.OneForgeHttpClientImplementation._

import scala.concurrent.{ ExecutionContext, Future }
import forex.interfaces.api.utils.ApiMarshallers._
import cats.syntax.functor._
import forex.interfaces.api.utils.HttpClient
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

trait OneForgeHttpClient {
  def retrieve(implicit executionContext: ExecutionContext,
               materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse]
}

class OneForgeHttpClientImplementation(
    httpClient: HttpClient,
    oneForgeUri: Uri,
) extends OneForgeHttpClient {

  def retrieve(implicit executionContext: ExecutionContext,
               materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse] =
    httpClient.getAs[OneForgeTextError, OneForgeResponse](oneForgeUri, OneForgeTextError(_))
}

object OneForgeHttpClientImplementation {

  sealed trait OneForgeResponse

  case class OneForgeListOfQuotes(
      quotes: List[OneForgeQuote]
  ) extends OneForgeResponse

  case class OneForgeJsonError(
      error: Boolean,
      message: String
  ) extends OneForgeResponse

  case class OneForgeTextError(
      message: String
  )

  case class OneForgeQuote(
      symbol: String,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      timestamp: Long
  )

  object OneForgeQuote {
    implicit val decoder: Decoder[OneForgeQuote] = deriveDecoder[OneForgeQuote]
  }

  object OneForgeResponse {
    private val OneForgeListOfQuotesDecoder: Decoder[OneForgeListOfQuotes] = Decoder.instance { cursor ⇒
      for {
        quotes ← cursor.as[List[OneForgeQuote]]
      } yield OneForgeListOfQuotes(quotes)
    }

    implicit val decoder: Decoder[OneForgeResponse] = List[Decoder[OneForgeResponse]](
      OneForgeListOfQuotesDecoder.widen,
      deriveDecoder[OneForgeJsonError].widen
    ).reduceLeft(_ or _)
  }

}
