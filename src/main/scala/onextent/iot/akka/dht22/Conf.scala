package onextent.iot.akka.dht22

import akka.actor.ActorSystem
import akka.pattern.AskTimeoutException
import akka.serialization.SerializationExtension
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

object Conf extends LazyLogging {

  val conf: Config = ConfigFactory.load()
  implicit val actorSystem: ActorSystem = ActorSystem("iotSystem", conf)
  SerializationExtension(actorSystem)

  val decider: Supervision.Decider = {

    case _: AskTimeoutException =>
      // might want to try harder, retry w/backoff if the actor is really supposed to be there
      logger.warn(s"decider discarding message to resume processing")
      //Supervision.Resume
      Supervision.Restart

    case e: java.text.ParseException =>
      logger.warn(
        s"decider discarding unparseable message to resume processing: $e")
      Supervision.Resume

    case e =>
      logger.error(s"decider can not decide: $e")
      Supervision.Restart

  }

  implicit val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))

  val mqttUrl: String = conf.getString("mqtt.url")
  val mqttUser: String = conf.getString("mqtt.user")
  val mqttPwd: String = conf.getString("mqtt.pwd")
  val mqttTopic: String = conf.getString("mqtt.topic")
  val mqttClientId: String = conf.getString("mqtt.clientId")

  val deviceId: String = conf.getString("main.deviceId")
  val intervalSeconds: FiniteDuration = Duration(conf.getInt("main.intervalSeconds"), SECONDS)

}

