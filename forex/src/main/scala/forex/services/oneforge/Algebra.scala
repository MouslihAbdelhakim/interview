package forex.services.oneforge

import forex.domain._
import forex.interfaces.api.utils.Error

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}
