package forex.processes.rates

import cats.Monad
import cats.data.EitherT
import forex.domain._
import forex.interfaces.api.utils.Error
import forex.services.OneForge

object Processes {
  def apply[F[_]]: Processes[F] =
    new Processes[F] {}
}

trait Processes[F[_]] {
  import messages._
  import converters._

  def get(
      request: GetRequest
  )(
      implicit
      M: Monad[F],
      OneForge: OneForge[F]
  ): F[Error Either Rate] =
    (for {
      result ← EitherT(OneForge.get(Rate.Pair(request.from, request.to))).leftMap(toProcessError(_))
    } yield result).value

}
