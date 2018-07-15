package onextent.iot.akka.dht22

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.akka.dht22.streams._

object Main extends LazyLogging {

  GpioFactory.setDefaultProvider(
    new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING))
  val gpio: GpioController = GpioFactory.getInstance()

  def main(args: Array[String]): Unit = {

    logger.info("starting...")

    TempAndHumidityReporter2()

  }

}
