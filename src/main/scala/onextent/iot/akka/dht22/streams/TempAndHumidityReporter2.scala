package onextent.iot.akka.dht22.streams

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.pi4j.io.gpio.RaspiBcmPin
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import onextent.iot.akka.dht22.Conf._
import onextent.iot.akka.dht22.io.{Dht22Sensor, SR04Sensor}
import onextent.iot.akka.dht22.models._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Akka Stream whose source is a temp and humidity module on a PI
  * and whose Sink reports to an https API
  */
object TempAndHumidityReporter2 extends LazyLogging {

  def createToConsumer(consumer: Sink[MqttMessage, Future[Done]])(
      implicit s: ActorSystem,
      m: Materializer): (Sink[MqttMessage, NotUsed], Future[Done]) = {
      MergeHub
        .source[MqttMessage](perProducerBufferSize = 16)
        .toMat(consumer)(Keep.both).run
  }

  def throttlingFlow[T]: Flow[T, T, NotUsed] =
    Flow[T].throttle(
      elements = 1,
      per = intervalSeconds,
      maximumBurst = 0,
      mode = ThrottleMode.Shaping
    )

  val connectionSettings: MqttConnectionSettings =
    MqttConnectionSettings(
      mqttUrl,
      mqttClientId,
      new MemoryPersistence
    ).withAuth(mqttUser, mqttPwd)

  val sinkSettings: MqttConnectionSettings =
    connectionSettings.withClientId(clientId = mqttClientId)

  def apply(): NotUsed = {

    def measureDistance() =
      (r: (Int, Command)) => (r._1, SR04Sensor())

    def readTemp() =
      (r: (Int, Command)) => (r._1, Dht22Sensor(r._1))

    def distReadings() =
      (t: (Int, Option[UltraSonicReading])) => {
        t._2 match {
          case Some(reading) =>
            List(UltraSonicReport(Some(s"$deviceId-temp-${t._1}"), reading))
          case _ =>
            logger.warn(s"empty ultrasonic reading")
            List()
        }
      }

    def tempReadings =
      (t: (Int, Option[TempReading])) => {
        t._2 match {
          case Some(reading) =>
            List(TempReport(Some(s"$deviceId-temp-${t._1}"), reading))
          case _ =>
            logger.warn(s"empty temp reading")
            List()
        }
      }

    val toConsumer = createToConsumer(
      MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    def mqttReading[T](implicit ioc: Encoder[T]) =
      (r: T) =>
        MqttMessage(mqttTopic,
                    ByteString(r.asJson.noSpaces),
                    Some(MqttQoS.AtLeastOnce),
                    retained = true)

    def handleTerminate(result: Future[Done]): Unit = {
      result onComplete {
        case Success(_) =>
          logger.warn("success. but stream should not end!")
          actorSystem.terminate()
        case Failure(e) =>
          logger.error(s"failure. stream should not end! $e", e)
          actorSystem.terminate()
      }
    }

    handleTerminate(toConsumer._2)

    //
    // begin streams
    //

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        // read port 4 every n seconds
        Source.fromGraph(new CommandSource(4)).via(throttlingFlow)
      }
      .map(readTemp())
      .mapConcat(tempReadings)
      .map(mqttReading)
      .to(toConsumer._1)
      .run()

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        // read temp port 4 when button is pressed
        Source.fromGraph(new ButtonSource(RaspiBcmPin.GPIO_02, 4))
      }
      .map(readTemp())
      .mapConcat(tempReadings)
      .map(mqttReading)
      .to(toConsumer._1)
      .run()

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        // read port 21 every n seconds
        Source.fromGraph(new CommandSource(21)).via(throttlingFlow)

      }
      .map(measureDistance())
      .mapConcat(distReadings())
      .map(mqttReading)
      .alsoTo(Sink.foreach(m => logger.debug(s"stream s3: ${new String(m.payload.toArray, "UTF8")}"))) // debug
      .to(toConsumer._1)
      .run()

  }

}
