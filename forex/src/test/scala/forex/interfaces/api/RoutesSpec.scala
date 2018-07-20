package forex.interfaces.api

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import forex.domain.Currency
import forex.interfaces.api.rates.Protocol.GetApiResponse
import org.scalatest.{FlatSpec, Matchers}
import forex.main.{Processes, Runners}
import org.scalatest.prop.TableDrivenPropertyChecks
import forex.interfaces.api.utils.Error.{ApiError, InternalError}

class RoutesSpec extends FlatSpec with Matchers with ScalatestRouteTest with TableDrivenPropertyChecks {

  import forex.interfaces.api.utils.ApiMarshallers._

  val forexApp = Routes(rates.Routes(Processes(), Runners())).route

  val currencyPairs = {
    val emptyTable = Table[Currency, Currency](
      ("from", "to")
    )

    val allCurrencyPairs = for {
      from <- Currency.listOfCurrencies
      to <- Currency.listOfCurrencies
    } yield (from, to)

    emptyTable ++ allCurrencyPairs
  }

  def checkHttpCodeStatus(route: Route, path: String, expectedCodeStatus: StatusCode) = {
    Get(path) ~> route ~> check {
      status shouldEqual expectedCodeStatus
    }
  }

  def checkThatPathReturnsAnApiError(route: Route, path: String) = {
    Get(path) ~> route ~> check {
      responseAs[ApiError] shouldBe a[ApiError]
    }
  }

  def checkThatPathReturnsAnInternalError(route: Route, path: String) = {
    Get(path) ~> route ~> check {
      responseAs[InternalError] shouldBe a[InternalError]
    }
  }

  def checkThatPathReturnsAGetApiResponse(route: Route, path: String) = {
    Get(path) ~> route ~> check {
      responseAs[GetApiResponse] shouldBe a[GetApiResponse]
    }
  }

  "Forex" should  "return Http code = BadRequest if Get query parameters `to` and `from` are not present" in
    checkHttpCodeStatus(forexApp, "/exchangeRate?", StatusCodes.BadRequest)

  it should "return Http code = BadRequest if Get query parameters `to` is not present" in
    checkHttpCodeStatus(forexApp, "/exchangeRate?from=USD&", StatusCodes.BadRequest)

  it should "return Http code = BadRequest if Get query parameters `from` is not present" in
    checkHttpCodeStatus(forexApp, "/exchangeRate?to=USD&", StatusCodes.BadRequest)

  it should "return Http code = BadRequest if Get query Parameter `from` contains an unsupported currency" in
  checkHttpCodeStatus(forexApp, "/exchangeRate?from=pennySilver&to=USD", StatusCodes.BadRequest)

  it should "return Http code = BadRequest if Get query Parameter `to` contains an unsupported currency" in
    checkHttpCodeStatus(forexApp, "/exchangeRate?from=USD&to=SilverPennies", StatusCodes.BadRequest)

  it should "return Http code = NotFound if path is anything but a `/`" in
    checkHttpCodeStatus(forexApp, "/path?from=USD&to=USD", StatusCodes.NotFound)

  it should "return Http code = OK for all supported currencies" in forAll(currencyPairs) {
    (from: Currency, to:Currency) ⇒ checkHttpCodeStatus(forexApp, s"/exchangeRate?from=$from&to=$to", StatusCodes.OK)
  }

  it should  "return a JSON object of ApiError if Get query parameters `to` and `from` are not present" in
    checkThatPathReturnsAnApiError(forexApp, "/exchangeRate?")

  it should "return a JSON object of ApiError if Get query parameters `to` is not present" in
    checkThatPathReturnsAnApiError(forexApp, "/exchangeRate?from=USD&")

  it should "return a JSON object of ApiError if Get query parameters `from` is not present" in
    checkThatPathReturnsAnApiError(forexApp, "/exchangeRate?to=USD&")

  it should "return a JSON object of ApiError if Get query Parameter `from` contains an unsupported currency" in
    checkThatPathReturnsAnApiError(forexApp, "/exchangeRate?from=pennySilver&to=USD")

  it should "return a JSON object of ApiError if Get query Parameter `to` contains an unsupported currency" in
    checkThatPathReturnsAnApiError(forexApp, "/exchangeRate?from=USD&to=SilverPennies")

  it should "return a JSON object of InternalError if path is anything but a `/`" in
    checkThatPathReturnsAnInternalError(forexApp, "/path?from=USD&to=USD")

  it should "return a JSON object of InternalError for all supported currencies" in forAll(currencyPairs) {
    (from: Currency, to:Currency) ⇒ checkThatPathReturnsAGetApiResponse(forexApp, s"/exchangeRate?from=$from&to=$to")
  }

  it should "return a JSON object with the same currency in the Get query parameters `from`" in forAll(currencyPairs) {
    (from: Currency, to:Currency) ⇒
      Get(s"/exchangeRate?from=$from&to=$to") ~> forexApp ~> check {
        val getApiResponse = responseAs[GetApiResponse]
        getApiResponse.from shouldBe from
      }
  }

  it should "return a JSON object with the same currency in the Get query parameters `to`" in forAll(currencyPairs) {
    (from: Currency, to:Currency) ⇒
      Get(s"/exchangeRate?from=$from&to=$to") ~> forexApp ~> check {
        val getApiResponse = responseAs[GetApiResponse]
        getApiResponse.to shouldBe to
      }
  }

}
