package forex.interfaces.api.utils

import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import Directives._
import forex.interfaces.api.utils.Error.ApiError

object ApiRejectionHandler {

  def apply(): server.RejectionHandler =
    RejectionHandler.newBuilder()
      .handleNotFound {
        extractUnmatchedPath { p ⇒
          import ApiMarshallers.marshaller
          complete(StatusCodes.NotFound -> ApiError(s"The path you requested [${p}] does not exist"))
        }
      }
      .result()

}
