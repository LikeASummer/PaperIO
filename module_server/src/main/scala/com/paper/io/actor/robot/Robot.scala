package com.paper.io.actor.robot

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.paper.io.Game
import com.paper.io.actor.session.Session.{InternalUpdate, SessionMessage}
import com.paper.io.geometry.base.Radians
import com.paper.io.geometry.complex.Field
import com.paper.io.ml.{Model, Sensor, TurnLeft, TurnRight}
import com.paper.io.player.PlayerId

object Robot {
  trait RobotMessage
  final case class Update(game: Game) extends RobotMessage

  def apply(parent: ActorRef[SessionMessage],
            playerId: PlayerId,
            model: Model,
            field: Field,
            rotationAngle: Radians = Radians(0F),
            delta: Float = 0.033F,
            frameCounter: Int = 0): Behavior[RobotMessage] = Behaviors.receive { case (_, message) =>
    message match {
      case Update(game) =>
        val frame = Sensor.sense(game.players(playerId), field, model.angles)
        val newModel = frame +: model
        var newFrameCounter = frameCounter

        val newRotationAngle = if (frameCounter == model.frameCount){
          newFrameCounter = 0
          model.decision() match {
            case TurnLeft => Radians(2F)
            case TurnRight => Radians(-2F)
            case _ => Radians(0F)
          }
        } else {
          newFrameCounter = newFrameCounter + 1
          rotationAngle
        }

        val player = game.players(playerId)
        val newDirection = player.direction.rotate(rotationAngle * delta)
        val newPosition = player.currentPosition + (newDirection * 10 * delta)

        parent ! InternalUpdate(newPosition, newDirection, playerId)
        apply(parent, playerId, newModel, field, newRotationAngle, delta, newFrameCounter)
    }
  }
}
