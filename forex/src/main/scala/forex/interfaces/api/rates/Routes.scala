package forex.interfaces.api.rates

import akka.http.scaladsl._
import forex.config._
import forex.main._
import forex.interfaces.api.utils._
import org.zalando.grafter.macros._
import cats.implicits._
import scala.util.{ Failure, Success }

@readerOf[ApplicationConfig]
case class Routes(
    processes: Processes
) {
  import server.Directives._
  import Directives._
  import Converters._
  import ApiMarshallers._

  import processes._

  lazy val exchangeRateRoute: server.Route =
    get {
      path("exchangeRate") {
        extractExecutionContext { implicit executor ⇒
          getApiRequest { maybeApiRequest ⇒
            val runnableApp = for {
              req ← maybeApiRequest
              getRequest ← toGetRequest(req)
            } yield
              Rates
                .get(getRequest)
                .map(_.map(toGetApiResponse(_)))

            runnableApp match {
              case Left(getApiError) ⇒
                complete(getApiError)

              case Right(futureMaybeResponse) ⇒
                onComplete(futureMaybeResponse) {
                  case Success(maybeResponse) ⇒
                    complete(maybeResponse)
                  case Failure(t) ⇒
                    complete(t)
                }
            }
          }
        }
      }
    }

}
