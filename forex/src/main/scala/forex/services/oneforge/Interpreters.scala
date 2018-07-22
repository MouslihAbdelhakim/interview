package forex.services.oneforge

import akka.actor.ActorRef
import scala.concurrent.Future

object Interpreters {
  def live(oneForgeBackEnd: ActorRef): Algebra[Future] = new LiveInterpreter(oneForgeBackEnd)
}
