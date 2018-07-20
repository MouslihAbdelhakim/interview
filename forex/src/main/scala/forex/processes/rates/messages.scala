package forex.processes.rates

import forex.domain._

package messages {
  final case class GetRequest(
      from: Currency,
      to: Currency
  )
}
