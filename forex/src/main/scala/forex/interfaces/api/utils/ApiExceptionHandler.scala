package forex.interfaces.api.utils

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import forex.interfaces.api.utils.Error.{ApiError, InternalError}

object ApiExceptionHandler {

  import ApiMarshallers.marshaller

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case error : ApiError ⇒
        ctx ⇒
          ctx.complete(StatusCodes.BadRequest -> error)

      case error: InternalError ⇒
        ctx ⇒
          ctx.complete(StatusCodes.InternalServerError -> error)

      case t: Throwable ⇒
        ctx ⇒
          ctx.complete(StatusCodes.InternalServerError -> InternalError(t.getMessage))
    }

}
