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

  val host: String = conf.getString("mqtt.publish.host")
  val proto: String = conf.getString("mqtt.publish.proto")
  val port: Int = conf.getInt("mqtt.publish.port")
  val mqttPublishUrl: String = s"$proto://$host:$port"

  val mqttPublishTopicPrefix: String = conf.getString("mqtt.publish.topicPrefix")
  val mqttPublishTopicSuffix: String = conf.getString("mqtt.publish.topicSuffix")
  val mqttTopic = s"$mqttPublishTopicPrefix$mqttPublishTopicSuffix"

  val mqttUser: String = conf.getString("mqtt.publish.user")
  val mqttPwd: String = conf.getString("mqtt.publish.pwd")
  val mqttClientId: String = conf.getString("main.appName") + "-" + conf.getString("mqtt.publish.clientId")
  val deviceId: String = conf.getString("main.deviceId")
  val intervalSeconds: FiniteDuration = Duration(conf.getInt("main.intervalSeconds"), SECONDS)

}

