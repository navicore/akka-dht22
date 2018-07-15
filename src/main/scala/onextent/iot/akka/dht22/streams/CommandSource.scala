package onextent.iot.akka.dht22.streams

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.akka.dht22.models._

class CommandSource(pin: Int)(implicit system: ActorSystem)
    extends GraphStage[SourceShape[(Int, Command)]] with LazyLogging {

  val out: Outlet[(Int, Command)] = Outlet("CommandSource")

  override val shape: SourceShape[(Int, Command)] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            logger.debug(s"command source for pin $pin onPull")
            push(out, (pin, ReadCommand()))
          }
        }
      )
    }

}
