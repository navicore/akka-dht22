package onextent.iot.akka.dht22.models

sealed trait Command
sealed trait Reading
sealed trait Report

case class ReadCommand() extends Command

case class TempReading(humidity: Option[Double] = None, temperature: Option[Double] = None) extends Reading
case class TempReport(DevAddr: Option[String] = Some("unknown"), data: TempReading) extends Report

case class UltraSonicReading(distance: Option[Double] = None) extends Reading
case class UltraSonicReport(DevAddr: Option[String] = Some("unknown"), data: UltraSonicReading) extends Report

case class Error()

