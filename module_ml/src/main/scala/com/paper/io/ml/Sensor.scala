package com.paper.io.ml

import cats.implicits._
import com.paper.io.geometry.base.{Position, Radians}
import com.paper.io.geometry.complex.Field
import com.paper.io.ml.Targetable.{TargetableField, TargetableTerritory, TargetableTrajectory}
import com.paper.io.player.Player

case class SensorFrame(playerInformation: SensorPlayer, targets: Vector[SensorTarget]) {
  def unwrap(): Array[Float] = playerInformation.unwrap() ++ targets.flatMap(_.unwrap()).toArray
}

case class SensorTarget(distance: Float, encodedType: TargetType, position: Position) {
  def unwrap(): Array[Float] = distance +: encodedType.code
}

case class SensorPlayer(normalizedX: Float, normalizedY: Float, normalizedZ: Float, isOutsideTerritory: Float, wasOutsideTerritory: Float) {
  def unwrap(): Array[Float] = Array(normalizedX, normalizedY, normalizedZ, isOutsideTerritory, wasOutsideTerritory)
}

object Sensor {
  def sense(player: Player, field: Field, angles: Array[Radians]): SensorFrame = {

    val sensorPlayer = SensorPlayer(player.currentPosition.x / field.radius.value, player.currentPosition.y / field.radius.value, 0.0f,
      if (player.territory.contains(player.currentPosition)) 0.0F else 1.0F,
      if (player.trajectory.isEmpty) 0.0F else 1.0F)

    val sensorTargets = angles.map(angle => Ray(player.currentPosition, player.direction.rotate(angle), 1000000L))
      .map(ray => findNearestVisibleTarget(player, field, ray)).toVector

    SensorFrame(sensorPlayer, sensorTargets)
  }

  private def findNearestVisibleTarget(player: Player, field: Field, ray: Ray): SensorTarget = {
    val allVisibleTargets = Vector(
      player.trajectory.target(ray).map(position => SensorTarget(position.distanceNormalized(ray.start, field.diameter), TargetOwnTrajectory, position)),
      player.territory.target(ray).map(position => SensorTarget(position.distanceNormalized(ray.start, field.diameter), TargetOwnTerritory, position)),
      field.target(ray).map(position => SensorTarget(position.distanceNormalized(ray.start, field.diameter), TargetGameField, position))
    ).flatten

    allVisibleTargets.minBy(target => target.distance)
  }
}
