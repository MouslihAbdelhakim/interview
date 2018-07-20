package forex

import forex.interfaces.api.utils.Error

package object services {

  type OneForge[F[_]] = oneforge.Algebra[F]
  final val OneForge = oneforge.Interpreters
  type OneForgeError = Error

}
