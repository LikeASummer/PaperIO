package com.paper.io.ml

import ai.onnxruntime.{OnnxTensor, OrtEnvironment, OrtSession}
import com.paper.io.geometry.base.Radians
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, parser}

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.util.Try

trait ModelDecision

case object TurnLeft extends ModelDecision
case object TurnRight extends ModelDecision
case object DoNothing extends ModelDecision
case object Unknown extends ModelDecision

object ModelDecision {
  def decode(decision: AnyRef): ModelDecision = {
    decision.asInstanceOf[Array[Array[Long]]].flatten match {
      case Array(0L) => TurnLeft
      case Array(1L) => TurnRight
      case Array(2L) => DoNothing
      case _ => Unknown
    }
  }
}

case class ModelConfiguration(path: String, angles: Array[Radians], history: Int, frameCount: Int)

object ModelConfiguration {

  implicit val radiansCodec: Codec.AsObject[Radians] = deriveCodec[Radians]
  implicit val modelConfigurationCodec: Codec.AsObject[ModelConfiguration] = deriveCodec[ModelConfiguration]

  def decode(jsonString: String): Try[ModelConfiguration] = {
    parser.decode[ModelConfiguration](jsonString).toTry
  }
}

case class Model(env: OrtEnvironment, session: OrtSession, history: History, angles: Array[Radians], frameCount: Int) extends AutoCloseable {
  def +:(sensorFrame: SensorFrame): Model = {
    copy(history = sensorFrame +: history)
  }

  def decision(): ModelDecision = {
    val dataTensor = OnnxTensor.createTensor(env, history.unwrap())

    val actionMasks: Array[Array[Float]] = Array(Array(3, 3, 3))
    val actionMasksTensor = OnnxTensor.createTensor(env, actionMasks)

    val inputs = new java.util.HashMap[String, OnnxTensor]()
    inputs.put("obs_0", dataTensor)
    inputs.put("action_masks", actionMasksTensor)

    ModelDecision.decode(session.run(inputs).get("discrete_actions").get().getValue)
  }

  override def close(): Unit = {
    Try(session.close())
    Try(env.close())
  }
}

object Model {
  def apply(path: String): Try[Model] = {

    for {
      byteRepresentation <- readResource(path)
      stringRepresentation <- Try(new String(byteRepresentation, StandardCharsets.UTF_8))
      modelConfiguration <- ModelConfiguration.decode(stringRepresentation)
      model <- readResource(modelConfiguration.path)
      env <- Try(OrtEnvironment.getEnvironment)
      session <- Try(env.createSession(model, new OrtSession.SessionOptions()))
    } yield new Model(env, session, History(modelConfiguration.history), modelConfiguration.angles, modelConfiguration.frameCount)
  }

  private def readResource(path: String): Try[Array[Byte]] = {
    Try {
      val stream: InputStream = getClass.getClassLoader.getResourceAsStream(path)
      val data = stream.readAllBytes
      stream.close()
      data
    }
  }
}
