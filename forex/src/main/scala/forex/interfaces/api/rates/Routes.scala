package forex.interfaces.api.rates

import akka.http.scaladsl._
import forex.config._
import forex.main._
import forex.interfaces.api.utils._
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Routes(
    processes: Processes,
    runners: Runners
) {
  import server.Directives._
  import Directives._
  import Converters._
  import ApiMarshallers._

  import processes._
  import runners._

  lazy val exchangeRateRoute: server.Route =
    get {
      path("exchangeRate") {
        getApiRequest { maybeApiRequest  ⇒
          val runnableApp = for {
            req ← maybeApiRequest
            getRequest ← toGetRequest(req)
          } yield runApp(
            Rates
              .get(getRequest)
              .map(_.map(result ⇒ toGetApiResponse(result)))
          )
          complete {
            runnableApp
          }
        }
      }
    }

}
