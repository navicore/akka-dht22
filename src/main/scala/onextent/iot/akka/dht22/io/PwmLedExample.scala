package onextent.iot.akka.dht22.io

import com.pi4j.io.gpio._

object PwmLedExample {

  val gpio: GpioController = GpioFactory.getInstance()

  val pin: Pin = RaspiBcmPin.GPIO_18
  val pwm: GpioPinPwmOutput = gpio.provisionPwmOutputPin(pin)
  pwm.setPwmRange(100)

  def apply(): Unit = {

    pwm.setPwm(0)

    for (i <- 1 to 100) {
      pwm.setPwm(i)
      Thread.sleep(10)
    }

    for (i <- 100 to 1 by -1) {
      pwm.setPwm(i)
      Thread.sleep(10)
    }

    pwm.setPwm(0)

  }

}
