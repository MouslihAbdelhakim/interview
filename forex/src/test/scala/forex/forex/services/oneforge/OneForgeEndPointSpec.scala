package forex.forex.services.oneforge

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestKit
import cats.data.EitherT
import forex.services.oneforge.OneForgeBackEnd.IncomingMessages.{ NoRatesRetrieved, RetrievedRates }
import forex.services.oneforge.OneForgeBackEnd.LoggableError
import forex.services.oneforge.{ OneForgeEndPoint, OneForgeHttpClient }
import forex.services.oneforge.OneForgeHttpClientImplementation._
import org.scalatest.{ FlatSpecLike, Matchers }

import scala.concurrent.{ ExecutionContext, Future }
import cats.implicits._
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.domain.Rate.Pair
import org.scalatest.concurrent.ScalaFutures

class OneForgeEndPointSpec
    extends TestKit(ActorSystem("OneForgeEndPointSpec"))
    with FlatSpecLike
    with Matchers
    with ScalaFutures {

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  "OneForgeEndPoint.refresh" should "return a NoRatesRetrieved if the http client returned a OneForgeTextError" in {
    val errorMessage = "ressources not found"
    val httpClient = new OneForgeHttpClient {
      override def retrieve(implicit executionContext: ExecutionContext,
                            materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse] =
        EitherT.left[OneForgeResponse] {
          Future(OneForgeTextError(errorMessage))
        }
    }

    val endPoint = new OneForgeEndPoint(httpClient)

    whenReady(endPoint.refresh) { retrievedInfo ⇒
      retrievedInfo shouldBe NoRatesRetrieved(List(LoggableError(errorMessage)))
    }
  }

  it should "return a NoRatesRetrieved if the http client returned a OneForgeJsonError" in {
    val errorMessage = "Api key not found"
    val httpClient = new OneForgeHttpClient {
      override def retrieve(implicit executionContext: ExecutionContext,
                            materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse] =
        EitherT.right[OneForgeTextError] {
          Future(OneForgeJsonError(true, errorMessage))
        }
    }

    val endPoint = new OneForgeEndPoint(httpClient)

    whenReady(endPoint.refresh) { retrievedInfo ⇒
      retrievedInfo shouldBe NoRatesRetrieved(
        List(LoggableError("No rates retrieved from oneForge"), LoggableError(errorMessage))
      )
    }
  }

  it should "return a NoRatesRetrieved if the http client returned no quotes" in {
    val httpClient = new OneForgeHttpClient {
      override def retrieve(implicit executionContext: ExecutionContext,
                            materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse] =
        EitherT.right[OneForgeTextError] {
          Future(OneForgeListOfQuotes(Nil))
        }
    }

    val endPoint = new OneForgeEndPoint(httpClient)

    whenReady(endPoint.refresh) { retrievedInfo ⇒
      retrievedInfo shouldBe NoRatesRetrieved(
        List(LoggableError("No rates retrieved from oneForge"))
      )
    }
  }

  it should "return a RetrievedRates with the quotes from the http client" in {

    val bid = 1
    val ask = 10
    val price = 20
    val expectedTimeStamp = Timestamp.now
    val expectedTimeStampInEpochSeconds = Timestamp.now.value.toEpochSecond
    val expectedPair = Pair(Currency.EUR, Currency.USD)
    val expectedRate = Rate(expectedPair, Price(bid), expectedTimeStamp)

    val httpClient = new OneForgeHttpClient {
      override def retrieve(implicit executionContext: ExecutionContext,
                            materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse] =
        EitherT.right[OneForgeTextError] {
          Future(
            OneForgeListOfQuotes(
              List(
                OneForgeQuote(
                  s"${expectedPair.to}${expectedPair.from}",
                  bid,
                  ask,
                  price,
                  expectedTimeStampInEpochSeconds
                )
              )
            )
          )
        }
    }

    val endPoint = new OneForgeEndPoint(httpClient)

    whenReady(endPoint.refresh) { retrievedInfo ⇒
      retrievedInfo shouldBe RetrievedRates(Map(expectedPair → expectedRate), expectedTimeStampInEpochSeconds, Nil)
    }
  }

  it should "include an error if a quote contains an unsupported currency pair" in {

    val bid = 1
    val ask = 10
    val price = 20
    val expectedTimeStamp = Timestamp.now
    val expectedTimeStampInEpochSeconds = Timestamp.now.value.toEpochSecond
    val expectedPair = Pair(Currency.EUR, Currency.USD)
    val expectedRate = Rate(expectedPair, Price(bid), expectedTimeStamp)

    val httpClient = new OneForgeHttpClient {
      override def retrieve(implicit executionContext: ExecutionContext,
                            materializer: Materializer): EitherT[Future, OneForgeTextError, OneForgeResponse] =
        EitherT.right[OneForgeTextError] {
          Future(
            OneForgeListOfQuotes(
              List(
                OneForgeQuote(
                  s"${expectedPair.to}${expectedPair.from}",
                  bid,
                  ask,
                  price,
                  expectedTimeStampInEpochSeconds
                ),
                OneForgeQuote(
                  s"pennies",
                  bid,
                  ask,
                  price,
                  expectedTimeStampInEpochSeconds
                )
              )
            )
          )
        }
    }

    val endPoint = new OneForgeEndPoint(httpClient)

    whenReady(endPoint.refresh) { retrievedInfo ⇒
      retrievedInfo shouldBe RetrievedRates(
        Map(expectedPair → expectedRate),
        expectedTimeStampInEpochSeconds,
        List(LoggableError("received an unusual currency pair: pennies"))
      )
    }
  }

}
