package forex.main

import forex.config._
import forex.services.OneForge
import forex.processes
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Processes() {

  implicit final lazy val _oneForge: OneForge[AppEffect] =
    OneForge.dummy[AppStack]

  final val Rates = processes.Rates[AppEffect]

}
