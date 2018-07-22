package forex.services.oneforge
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import forex.domain.Rate
import forex.services.OneForgeError
import forex.services.oneforge.OneForgeBackEnd.IncomingMessages.ExchangeRateFor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class LiveInterpreter(
    oneForgeEndPoint: ActorRef
) extends Algebra[Future] {
  override def get(pair: Rate.Pair): Future[Either[OneForgeError, Rate]] = {
    implicit val timeout = Timeout(3 seconds)
    ask(oneForgeEndPoint, ExchangeRateFor(pair)).mapTo[Future[Either[OneForgeError, Rate]]].flatten
  }

}
