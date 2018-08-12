package forex.interfaces.api

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import forex.domain.{ Currency, Rate, Timestamp }
import forex.interfaces.api.rates.Protocol.GetApiResponse
import forex.interfaces.api.utils.Error
import org.scalatest.{ FlatSpec, Matchers }
import forex.main.Processes
import org.scalatest.prop.TableDrivenPropertyChecks
import forex.interfaces.api.utils.Error.{ ApiError, BackEndError, InternalError }
import forex.processes
import forex.services.OneForge

import scala.concurrent.Future
import scala.reflect.ClassTag

class RoutesSpec extends FlatSpec with Matchers with ScalatestRouteTest with TableDrivenPropertyChecks {

  import forex.interfaces.api.utils.ApiMarshallers._

  private val successfulProcess = new Processes {
    override implicit val _oneForge: OneForge[Future] = new OneForge[Future] {
      override def get(pair: Rate.Pair): Future[Either[Error.BackEndError, Rate]] = Future {
        Right(Rate(pair.from, pair.to, 1, Timestamp.now.value))
      }
    }
    override val Rates: processes.rates.Processes[Future] = processes.Rates[Future]
  }

  private val unsuccessfulProcces = new Processes {
    override implicit val _oneForge: OneForge[Future] = new OneForge[Future] {
      override def get(pair: Rate.Pair): Future[Either[Error.BackEndError, Rate]] = Future {
        Left(BackEndError("1forge is inaccessible"))
      }
    }
    override val Rates: processes.rates.Processes[Future] = processes.Rates[Future]
  }

  private val forexApp = Routes(rates.Routes(successfulProcess)).route
  private val forexAppWithInaccessible1Forge = Routes(rates.Routes(unsuccessfulProcces)).route

  private val currencyPairs = {
    val emptyTable = Table[Currency, Currency](
      ("from", "to")
    )

    val allCurrencyPairs = for {
      from ← Currency.listOfCurrencies
      to ← Currency.listOfCurrencies
    } yield (from, to)

    emptyTable ++ allCurrencyPairs
  }

  def checkHttpCodeStatus(route: Route, path: String, expectedCodeStatus: StatusCode) =
    Get(path) ~> route ~> check {
      status shouldEqual expectedCodeStatus
    }

  def checkThatPathReturnsA[ReturnType: FromResponseUnmarshaller: ClassTag](route: Route, path: String) =
    Get(path) ~> route ~> check {
      responseAs[ReturnType] shouldBe a[ReturnType]
    }

  "Forex" should "return Http code = BadRequest if Get query parameters `to` and `from` are not present" in
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
    (from: Currency, to: Currency) ⇒
      checkHttpCodeStatus(forexApp, s"/exchangeRate?from=$from&to=$to", StatusCodes.OK)
  }

  it should "return Http code = InternalServerError when encountered an error when retrieving a rate" in forAll(
    currencyPairs
  ) { (from: Currency, to: Currency) ⇒
    checkHttpCodeStatus(
      forexAppWithInaccessible1Forge,
      s"/exchangeRate?from=$from&to=$to",
      StatusCodes.InternalServerError
    )
  }

  it should "return a JSON object of ApiError if Get query parameters `to` and `from` are not present" in
    checkThatPathReturnsA[ApiError](forexApp, "/exchangeRate?")

  it should "return a JSON object of ApiError if Get query parameters `to` is not present" in
    checkThatPathReturnsA[ApiError](forexApp, "/exchangeRate?from=USD&")

  it should "return a JSON object of ApiError if Get query parameters `from` is not present" in
    checkThatPathReturnsA[ApiError](forexApp, "/exchangeRate?to=USD&")

  it should "return a JSON object of ApiError if Get query Parameter `from` contains an unsupported currency" in
    checkThatPathReturnsA[ApiError](forexApp, "/exchangeRate?from=pennySilver&to=USD")

  it should "return a JSON object of ApiError if Get query Parameter `to` contains an unsupported currency" in
    checkThatPathReturnsA[ApiError](forexApp, "/exchangeRate?from=USD&to=SilverPennies")

  it should "return a JSON object of InternalError if path is anything but a `/`" in
    checkThatPathReturnsA[InternalError](forexApp, "/path?from=USD&to=USD")

  it should "return a JSON object of GetApiResponse for all supported currencies" in forAll(currencyPairs) {
    (from: Currency, to: Currency) ⇒
      checkThatPathReturnsA[GetApiResponse](forexApp, s"/exchangeRate?from=$from&to=$to")
  }

  it should "return a Json object of BackEndError when encountered an error when retrieving a rate" in forAll(
    currencyPairs
  ) { (from: Currency, to: Currency) ⇒
    checkThatPathReturnsA[BackEndError](forexAppWithInaccessible1Forge, s"/exchangeRate?from=$from&to=$to")
  }

  it should "return a JSON object with the same currency in the Get query parameters `from`" in forAll(currencyPairs) {
    (from: Currency, to: Currency) ⇒
      Get(s"/exchangeRate?from=$from&to=$to") ~> forexApp ~> check {
        val getApiResponse = responseAs[GetApiResponse]
        getApiResponse.from shouldBe from
      }
  }

  it should "return a JSON object with the same currency in the Get query parameters `to`" in forAll(currencyPairs) {
    (from: Currency, to: Currency) ⇒
      Get(s"/exchangeRate?from=$from&to=$to") ~> forexApp ~> check {
        val getApiResponse = responseAs[GetApiResponse]
        getApiResponse.to shouldBe to
      }
  }
}
