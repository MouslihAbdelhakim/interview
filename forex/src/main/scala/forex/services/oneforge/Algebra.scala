package forex.services.oneforge

import forex.domain._
import forex.interfaces.api.utils.Error.BackEndError

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[BackEndError Either Rate]
}
