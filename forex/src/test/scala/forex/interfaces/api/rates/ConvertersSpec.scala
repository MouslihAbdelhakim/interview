package forex.interfaces.api.rates

import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.domain.Rate.Pair
import forex.interfaces.api.rates.Protocol.GetApiRequest
import forex.processes.rates.messages.GetRequest
import forex.services.oneforge.OneForgeHttpClientImplementation.OneForgeQuote
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{ FlatSpec, Matchers }

class ConvertersSpec extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  import forex.domain.Currency._

  "Converters.toGetRequest" should "return a Right(GetRequest) when both currencies are supported" in {
    Converters.toGetRequest(GetApiRequest(Right(EUR), Right(USD))) shouldBe Right(GetRequest(EUR, USD))
  }

  val badQueryParams = Table(
    ("from", "to"),
    (Left(""), Right(USD)),
    (Right(USD), Left("")),
    (Left(""), Left(""))
  )

  it should "return Left(ApiError) if either from or either are left" in forAll(badQueryParams) {
    (from: Either[String, Currency], to: Either[String, Currency]) ⇒
      Converters.toGetRequest(GetApiRequest(from, to)) shouldBe a[Left[_, _]]
  }

  "Converters.toRate" should "should return the base currency as the target currency,the counter currency as the source currency and the bid as the price" in {
    val counterCurrency = USD
    val baseCurrency = EUR
    val expectedTimeStamp = Timestamp.now
    val timeStamp = expectedTimeStamp.value.toEpochSecond
    val bid = 1
    val ask = 10
    val price = 20
    val oneForgeQuote = OneForgeQuote(s"$baseCurrency$counterCurrency", bid, price, ask, timeStamp)
    val expectedRate = Rate(Pair(counterCurrency, baseCurrency), Price(bid), expectedTimeStamp)
    Converters.toRate(oneForgeQuote) shouldBe Right(expectedRate)
  }

  it should "return a left if the currency pair contains an unsupported currency" in {
    val timeStamp = Timestamp.now.value.toEpochSecond
    val oneForgeQuote = OneForgeQuote("pennies", 1, 2, 3, timeStamp)
    Converters.toRate(oneForgeQuote) shouldBe a[Left[_, _]]
  }

  it should "return a left if the currency pair is empty" in {
    val timeStamp = Timestamp.now.value.toEpochSecond
    val oneForgeQuote = OneForgeQuote("", 1, 2, 3, timeStamp)
    Converters.toRate(oneForgeQuote) shouldBe a[Left[_, _]]
  }

}
