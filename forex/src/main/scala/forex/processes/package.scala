package forex

import forex.interfaces.api.utils.Error

package object processes {

  type Rates[F[_]] = rates.Processes[F]
  final val Rates = rates.Processes
  type RatesError = Error
  final val RatesError = interfaces.api.utils.Error

}
