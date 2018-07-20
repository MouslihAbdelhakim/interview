package forex.processes.rates

import forex.interfaces.api.utils.Error

package object converters {

  def toProcessError[T <: Throwable](t: T): Error = t match {
    case error: Error               ⇒ error
    case e                          ⇒ Error.InternalError(e.getMessage)
  }

}
