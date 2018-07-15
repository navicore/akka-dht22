package onextent.iot.akka.dht22.io

import com.pi4j.io.gpio._
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.akka.dht22.models.UltraSonicReading

object SR04Sensor extends LazyLogging {

  val TIMEOUT: Int = 2100 * 10

  val gpio: GpioController = GpioFactory.getInstance()

  val sensorTriggerPin: GpioPinDigitalOutput =
    gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_20,
                                   "pin_trig",
                                   PinState.HIGH)

  val sensorEchoPin: GpioPinDigitalInput =
    gpio.provisionDigitalInputPin(RaspiBcmPin.GPIO_21,
                                  PinPullResistance.PULL_DOWN)

  def measure(f: () => Boolean): Long = {
    val startTime = System.nanoTime
    var countDown = TIMEOUT
    while (f()) {
      busyWaitNanos(1)
      countDown = countDown - 1
      if (countDown == 0) throw new Exception("timeout")
    }
    System.nanoTime - startTime
  }

  def busyWaitMicros(micros: Long): Unit = {
    val waitUntil = System.nanoTime + (micros * 1000)
    while ({
      waitUntil > System.nanoTime
    }) {}
  }

  def busyWaitNanos(nanos: Long): Unit = {
    val waitUntil = System.nanoTime + nanos
    while ({
      waitUntil > System.nanoTime
    }) {}
  }

  private def triggerSensor(): Unit = {
    sensorTriggerPin.low()
    busyWaitMicros(2)
    sensorTriggerPin.high()
    busyWaitMicros(10)
    sensorTriggerPin.low()
  }

  def apply(): Option[UltraSonicReading] = {

    logger.debug(s"measure distance with sensor ...")

    try {

      triggerSensor()

      measure(() => {
        sensorEchoPin.isLow
      })
      logger.debug("ejs 2 got startTime")

      val duration = measure(() => sensorEchoPin.isHigh)

      logger.debug("ejs 3 got stopTime")

      val result: Double = ((duration / 1e3) / 2) / 29.1

      logger.debug(s"... distance by sensor was $result")
      Some(UltraSonicReading(Some(result)))

    } catch {
      //case e: InterruptedException =>
      case e: Throwable =>
        //logger.warn(s"$e", e)
        logger.warn(s"$e")
        None
    }

  }

}
