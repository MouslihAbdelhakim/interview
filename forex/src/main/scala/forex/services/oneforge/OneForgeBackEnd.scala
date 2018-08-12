package forex.services.oneforge

import akka.actor.{ Actor, ActorLogging, Props, Stash }
import akka.stream.ActorMaterializer
import forex.domain.Rate.Pair
import forex.domain.{ Price, Rate, Timestamp }
import forex.interfaces.api.utils.Error.BackEndError
import forex.services.oneforge.OneForgeBackEnd.IncomingMessages._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class OneForgeBackEnd(oneForgeEndPoint: OneForgeEndPoint,
                      timeToWaitBeforeRefreshingInfo: Long,
                      timeToWaitBeforeRetry: Long)
    extends Actor
    with Stash
    with ActorLogging {

  implicit val materialize = ActorMaterializer()
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = waitingForOneForge(oneForgeEndPoint.refresh)

  private def waitingForOneForge(notYetInformation: Future[InformationRetrievedFromOneForge]): Receive = {
    def serveFromNotYetAvailableInformation(
        informationRetrievedFromOneForge: InformationRetrievedFromOneForge,
        pair: Pair
    ): Either[BackEndError, Rate] = informationRetrievedFromOneForge match {
      case RetrievedRates(availableRates, _, _) ⇒
        getRate(availableRates, pair)
      case _ ⇒
        Left(BackEndError("unfortunately our third party providers are not available, please retry later"))
    }

    import akka.pattern.pipe
    notYetInformation.pipeTo(self)

    val behavior: Receive = {
      case NoRatesRetrieved(errors) ⇒
        errors.foreach(error ⇒ log.error(error.message))
        context.system.scheduler.scheduleOnce(timeToWaitBeforeRetry.seconds, self, RetryToRetrieveOneForgeInformation)
        () // returning unit

      case RetryToRetrieveOneForgeInformation ⇒
        context.become(waitingForOneForge(oneForgeEndPoint.refresh))

      case RetrievedRates(availableRates, oldestTimeStamp, errors) ⇒
        errors.foreach(error ⇒ log.error(error.message))
        context.become(servingExchangeRatesForAWhile(availableRates, oldestTimeStamp, timeToWaitBeforeRefreshingInfo))

      case ExchangeRateFor(pair) ⇒
        notYetInformation.map(serveFromNotYetAvailableInformation(_, pair)).pipeTo(sender())
        () // returning Unit
    }

    unstashAll()
    behavior
  }

  private def servingExchangeRatesForAWhile(availableRates: Map[Pair, Rate],
                                            lastTimeInfoWasRetrieved: Long,
                                            timeToWaitBeforeRefreshingInfo: Long): Receive = {
    case ExchangeRateFor(pair) ⇒
      val now = Timestamp.now.value.toEpochSecond
      if (now - lastTimeInfoWasRetrieved < timeToWaitBeforeRefreshingInfo) {
        sender() ! getRate(availableRates, pair)
      } else {
        stash()
        context.become(waitingForOneForge(oneForgeEndPoint.refresh))
      }
  }

  private def getRate(availableRates: Map[Pair, Rate], pair: Pair): Either[BackEndError, Rate] =
    if (pair.from == pair.to) {
      Right(Rate(pair, Price(1), Timestamp.now))
    } else {
      availableRates
        .get(pair)
        .toRight(BackEndError(s"unfortunately our third party providers do not support ${pair}"))
    }

}

object OneForgeBackEnd {

  def props(oneForgeEndPoint: OneForgeEndPoint,
            refreshInSeconds: Long,
            compensateInSeconds: Long,
            timeToWaitBeforeRetry: Long): Props =
    Props(classOf[OneForgeBackEnd], oneForgeEndPoint, refreshInSeconds - compensateInSeconds, timeToWaitBeforeRetry)

  object IncomingMessages {
    case class ExchangeRateFor(pair: Pair)

    sealed trait InformationRetrievedFromOneForge

    case class RetrievedRates(
        mapOfAvailableRates: Map[Pair, Rate],
        oldestTimeStamp: Long,
        UnparsedCurrencyPairsErrors: List[LoggableError]
    ) extends InformationRetrievedFromOneForge

    case class NoRatesRetrieved(
        errors: List[LoggableError]
    ) extends InformationRetrievedFromOneForge

    case object RetryToRetrieveOneForgeInformation
  }

  case class LoggableError(message: String)

}
