package forex.services.oneforge

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.{ Host, Path, Query }
import forex.config.OneForgeApiConfig
import forex.domain.Currency

class OneForgeUri(
    protocol: String,
    host: Host,
    path: Path,
    apiKey: String,
    listOfCurrencies: List[Currency]
) {

  def get: Uri = {
    val allCurrencyPairsParameterValue = (for {
      baseCurrency ← listOfCurrencies
      counterCurrency ← listOfCurrencies if (baseCurrency != counterCurrency)
    } yield s"$baseCurrency$counterCurrency").mkString(",")
    val currencyPairsAndKeyQuery = Query(Map("pairs" → allCurrencyPairsParameterValue, "api_key" → apiKey))

    Uri./.withScheme(protocol)
      .withHost(host)
      .withPath(path)
      .withQuery(currencyPairsAndKeyQuery)

  }

}

object OneForgeUri {
  def apply(config: OneForgeApiConfig, listOfCurrencies: List[Currency]): OneForgeUri =
    new OneForgeUri(config.protocol, Host(config.host), Path(config.path), config.key, listOfCurrencies)
}
