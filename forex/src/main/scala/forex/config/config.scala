package forex.config

import org.zalando.grafter.macros._
import scala.concurrent.duration.FiniteDuration

@readers
case class ApplicationConfig(
    akka: AkkaConfig,
    api: ApiConfig,
    executors: ExecutorsConfig,
    oneForgeApi: OneForgeApiConfig,
    cache: CacheConfig
)

case class AkkaConfig(
    name: String,
    exitJvmTimeout: Option[FiniteDuration]
)

case class ApiConfig(
    interface: String,
    port: Int
)

case class ExecutorsConfig(
    default: String
)

case class OneForgeApiConfig(
    protocol: String,
    host: String,
    path: String,
    key: String,
    retryInSeconds: Long
)

case class CacheConfig(
    refreshInSeconds: Long,
    compensateInSeconds: Long
)
