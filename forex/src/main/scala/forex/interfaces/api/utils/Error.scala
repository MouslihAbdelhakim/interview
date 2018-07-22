package forex.interfaces.api.utils

import io.circe._
import io.circe.generic.semiauto._

import scala.util.control.NoStackTrace

sealed trait Error extends Throwable with NoStackTrace
object Error {
  final case class ApiError(message: String) extends Error
  final case class InternalError(message: String) extends Error
  final case class BackEndError(error: String) extends Error

  object ApiError {
    implicit val encoder: Encoder[ApiError] =
      deriveEncoder[ApiError]

    implicit val decoder: Decoder[ApiError] =
      deriveDecoder[ApiError]
  }

  object InternalError {
    implicit val encoder: Encoder[InternalError] =
      deriveEncoder[InternalError]

    implicit val decoder: Decoder[InternalError] =
      deriveDecoder[InternalError]
  }

  object BackEndError {
    implicit val encoder: Encoder[BackEndError] =
      deriveEncoder[BackEndError]

    implicit val decoder: Decoder[BackEndError] =
      deriveDecoder[BackEndError]
  }

}
