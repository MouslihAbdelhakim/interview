package forex.interfaces.api.utils

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, ResponseEntity, Uri }
import akka.http.scaladsl.unmarshalling.{ Unmarshal, Unmarshaller }
import akka.stream.Materializer
import cats.data.EitherT
import cats.implicits._

import scala.concurrent.{ ExecutionContext, Future }

class HttpClient(http: HttpExt) {
  def getAs[A, B](url: Uri, textWrapper: String ⇒ A)(implicit
                                                     unmarshallerB: Unmarshaller[ResponseEntity, B],
                                                     executionContext: ExecutionContext,
                                                     materializer: Materializer): EitherT[Future, A, B] = {
    def processHttpResponse(httpResponse: HttpResponse): EitherT[Future, A, B] =
      if (httpResponse.status.isSuccess) {
        EitherT.right {
          for {
            b ← Unmarshal(httpResponse.entity).to[B]
          } yield b
        }
      } else {
        EitherT.left {
          for {
            message ← Unmarshal(httpResponse.entity).to[String]
          } yield textWrapper(message)
        }
      }

    for {
      response ← EitherT.right(http.singleRequest(HttpRequest(uri = url)))
      targetEither ← processHttpResponse(response)
    } yield targetEither

  }
}
