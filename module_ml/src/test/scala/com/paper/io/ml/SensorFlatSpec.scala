package com.paper.io.ml

import com.paper.io.geometry.base.{Direction, Position, Radians}
import com.paper.io.geometry.complex.{Field, Territory, Trajectory}
import com.paper.io.geometry.utils.GeometryUtils
import com.paper.io.player.{Player, PlayerId}
import org.scalatest.flatspec.AnyFlatSpec

class SensorFlatSpec extends AnyFlatSpec {
  "Sensor" should "find trajectory" in {
    val field = Field(100)
    val player = Player(PlayerId(), Direction(1, 0), Position(50, 50), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
      .copy(trajectory = Trajectory(Position(10, 0), Position(30, 0), Position(60, 0), Position(60, 60)))

    val sensorFrame = Sensor.sense(player, field, Array(Radians(0)))
    assert(sensorFrame.targets.head.encodedType == TargetOwnTrajectory)
  }

  "Sensor" should "find territory" in {
    val field = Field(100)
    val player = Player(PlayerId(), Direction(1, 0), Position(), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))

    val sensorFrame = Sensor.sense(player, field, Array(Radians(0)))
    assert(sensorFrame.targets.head.encodedType == TargetOwnTerritory)
  }

  "Sensor" should "find field" in {
    val field = Field(100)
    val player = Player(PlayerId(), Direction(1, 0), Position(50, 50), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))

    val sensorFrame = Sensor.sense(player, field, Array(Radians(0)))
    assert(sensorFrame.targets.head.encodedType == TargetGameField)
  }
}
