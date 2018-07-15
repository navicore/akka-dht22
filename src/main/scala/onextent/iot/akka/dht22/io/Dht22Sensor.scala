package onextent.iot.akka.dht22.io

import io.circe.generic.auto._
import onextent.iot.dht22.Dht22
import onextent.iot.akka.dht22.models.TempReading

object Dht22Sensor {

  def apply(pin: Int): Option[TempReading] = {

    synchronized {

      PwmLedExample()

      val dht22 = new Dht22

      val reading = dht22.get(pin)

      if (reading(0) > 0 && reading(2) > 0) {
        Some(TempReading(Some(reading(2).toDouble), Some(reading(0).toDouble)))
      } else {
        None
      }
    }

  }

}
