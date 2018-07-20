package forex.interfaces.api.rates

import akka.http.scaladsl._
import forex.domain._
import forex.interfaces.api.utils.Error.ApiError

trait Directives {
  import server.Directives._
  import Protocol._

  def getApiRequest: server.Directive1[Either[ApiError, GetApiRequest]] = {
    val maybeFromAndTo = for {
      maybeFrom ← parameter('from.?)
      maybeTo ← parameter('to.?)
    } yield for {
      from ← maybeFrom
      to ← maybeTo
    } yield GetApiRequest(Currency.fromString(from), Currency.fromString(to))
    maybeFromAndTo.map(_.toRight(ApiError("Both Get parameters `from` and `to` are mandatory")))
  }

}

object Directives extends Directives
