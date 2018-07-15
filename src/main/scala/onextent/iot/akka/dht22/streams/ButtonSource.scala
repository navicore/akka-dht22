package onextent.iot.akka.dht22.streams

import java.util.concurrent.ArrayBlockingQueue

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.akka.dht22.models._

class ButtonSource(buttonPin: Pin, tempPin: Int)(implicit system: ActorSystem)
    extends GraphStage[SourceShape[(Int, Command)]]
    with LazyLogging {

  val out: Outlet[(Int, Command)] = Outlet("ButtonSource")

  override val shape: SourceShape[(Int, Command)] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {

    val bQueue = new ArrayBlockingQueue[ReadCommand](1)
    val gpio: GpioController = GpioFactory.getInstance

    val myButton: GpioPinDigitalInput =
      gpio.provisionDigitalInputPin(buttonPin, PinPullResistance.PULL_DOWN)

    myButton.addListener(new GpioPinListenerDigital() {
      override def handleGpioPinDigitalStateChangeEvent(
          event: GpioPinDigitalStateChangeEvent): Unit = {
        if (event.getState == PinState.LOW)
          try {
            logger.debug(s"button $buttonPin pressed")
            bQueue.offer(ReadCommand(), 10, java.util.concurrent.TimeUnit.MINUTES)
            logger.debug(s"button $buttonPin offer accepted")
          } catch {
            case e: Throwable => logger.debug(s"offer: $e")
          }

      }
    })

    new GraphStageLogic(shape) {

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            logger.debug(s"button $buttonPin onPull")
            try {
              push(
                out,
                (tempPin, bQueue.poll(10, java.util.concurrent.TimeUnit.MINUTES)))
            } catch {
              case e: Throwable => logger.debug(s"poll: $e")
            }
          }
        }
      )
    }

  }

}
