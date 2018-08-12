package forex.main

import akka.http.scaladsl.Http
import forex.config._
import forex.domain.Currency
import forex.interfaces.api.utils.HttpClient
import forex.services.OneForge
import forex.processes
import forex.services.oneforge._
import org.zalando.grafter.macros._

import scala.concurrent.Future

@defaultReader[OneForgeProcess]
trait Processes {
  implicit val _oneForge: OneForge[Future]
  val Rates: forex.processes.rates.Processes[Future]
}

@readerOf[ApplicationConfig]
case class OneForgeProcess(
    oneForgeApiConfig: OneForgeApiConfig,
    actorSystems: ActorSystems,
    cacheConfig: CacheConfig
) extends Processes {

  implicit override final lazy val _oneForge: OneForge[Future] = {
    val system = actorSystems.system
    val oneForgeUri = OneForgeUri(oneForgeApiConfig, Currency.listOfCurrencies)
    val httpClient = new HttpClient(Http(system))
    OneForge.live {
      system.actorOf(
        OneForgeBackEnd.props(
          new OneForgeEndPoint(new OneForgeHttpClientImplementation(httpClient, oneForgeUri.get)),
          cacheConfig.refreshInSeconds,
          cacheConfig.compensateInSeconds,
          oneForgeApiConfig.retryInSeconds
        )
      )
    }
  }

  final override val Rates = processes.Rates[Future]

}
