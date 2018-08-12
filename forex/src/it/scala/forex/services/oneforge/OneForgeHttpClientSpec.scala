package forex.services.oneforge

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import forex.config.OneForgeApiConfig
import forex.domain.Currency
import org.scalatest.{ FlatSpecLike, Matchers }
import forex.interfaces.api.utils.HttpClient
import forex.services.oneforge.OneForgeHttpClientImplementation.{ OneForgeJsonError, OneForgeListOfQuotes }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }

class OneForgeHttpClientSpec
    extends TestKit(ActorSystem("OneForgeEndPointSpec"))
    with FlatSpecLike
    with Matchers
    with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(3, Seconds), interval = Span(5, Millis))

  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  private val oneForgeConfig = pureconfig.loadConfig[OneForgeApiConfig]("oneforge").right.get
  private val httpClient = new HttpClient(Http(system))

  "OneForgeHttpClientImplementation.retrieve" should
    "return a OneForgeListOfQuotes when both the currency pairs and api are valid" in {
    val uri = OneForgeUri(oneForgeConfig, Currency.listOfCurrencies).get
    val oneForgeHttpClient =
      new OneForgeHttpClientImplementation(httpClient, uri)

    whenReady(oneForgeHttpClient.retrieve.value) {
      case Right(OneForgeListOfQuotes(quotes)) ⇒
        val receivedCurrencyPairs = quotes.map(_.symbol).toSet

        val expectedCurrencyPairs = (for {
          base ← Currency.listOfCurrencies
          counter ← Currency.listOfCurrencies if (base != counter)
        } yield s"${base}${counter}").toSet

        receivedCurrencyPairs shouldBe expectedCurrencyPairs

      case response ⇒
        fail(s"didn't receive a OneForgeListOfQuotes, instead received : $response")

    }
  }

  it should "return a OneForgeJsonError when the API key is invalid" in {
    val configWithInvalidKey = oneForgeConfig.copy(key = "")
    val uri = OneForgeUri(configWithInvalidKey, Currency.listOfCurrencies).get
    val oneForgeHttpClient =
      new OneForgeHttpClientImplementation(httpClient, uri)

    whenReady(oneForgeHttpClient.retrieve.value) {
      case Right(OneForgeJsonError(_, _)) ⇒
        succeed
      case response ⇒
        fail(s"didn't receive a OneForgeJsonError, instead received : $response")
    }

  }

  it should "return a OneForgeTextError when requesting a URI with an invalid path" in {
    val configWithInvalidPath = oneForgeConfig.copy(path = "/gandalf")
    val uri = OneForgeUri(configWithInvalidPath, Currency.listOfCurrencies).get
    val oneForgeHttpClient =
      new OneForgeHttpClientImplementation(httpClient, uri)

    whenReady(oneForgeHttpClient.retrieve.value) { response ⇒
      response shouldBe a[Left[_, _]]
    }
  }

  it should "return a OneForgeTextError when 1forge is inaccessible" in {
    val configWithInvalidHost = oneForgeConfig.copy(host = "www.example.com")
    val uri = OneForgeUri(configWithInvalidHost, Currency.listOfCurrencies).get
    val oneForgeHttpClient =
      new OneForgeHttpClientImplementation(httpClient, uri)

    whenReady(oneForgeHttpClient.retrieve.value) { response ⇒
      response shouldBe a[Left[_, _]]
    }
  }

}
