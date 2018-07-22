package forex.forex.services.oneforge

import akka.http.scaladsl.model.Uri.{ Host, Path }
import forex.config.OneForgeApiConfig
import forex.domain.Currency.{ EUR, USD }
import forex.services.oneforge.OneForgeUri
import org.scalatest.{ FlatSpec, Matchers }

class OneForgeUriSpec extends FlatSpec with Matchers {

  object testData {
    val scheme = "https"
    val host = "www.example.com"
    val path = "/path/to/resource"
    val apikey = "key"
    val config = OneForgeApiConfig(scheme, host, path, apikey, 0)
    val currencies = List(EUR, USD)
    val uri = OneForgeUri(config, currencies)
  }

  "OneForgeUri.get" should "return the url to oneForge using the provided config" in {
    import testData._
    uri.get
      .toString() shouldBe s"""$scheme://$host$path?pairs=${EUR.toString + USD.toString},${USD.toString + EUR.toString}&api_key=$apikey"""
  }

  it should "return a uri with the provided Scheme" in {
    import testData._
    uri.get.scheme shouldBe scheme
  }

  it should "return a uri with the provided Host" in {
    import testData._
    uri.get.authority.host shouldBe Host(host)
  }

  it should "return a uri with the provided path" in {
    import testData._
    uri.get.path shouldBe Path(path)
  }

  it should "return a uri with a Query constructed from the currencies and the key" in {
    import testData._
    uri.get.queryString() shouldBe Some(
      s"pairs=${EUR.toString + USD.toString},${USD.toString + EUR.toString}&api_key=$apikey"
    )
  }
}
