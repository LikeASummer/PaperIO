package com.paper.io

import com.paper.io.geometry.base.{Direction, Position}
import com.paper.io.geometry.complex.{Territory, Trajectory}
import com.paper.io.geometry.special.EmptyOwner
import com.paper.io.geometry.utils.GeometryUtils
import com.paper.io.player.{Player, PlayerId}
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.flatspec.AnyFlatSpec

class PlayerFlatSpec extends AnyFlatSpec{
  "Player" should "be created" in {
    val player = Player(PlayerId(), Direction(1, 0), Position(), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
    assert(player.isActive)
  }

  "Player" should "not have trajectory when still inside territory" in {
    var player = Player(PlayerId(), Direction(1, 0), Position(), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
    for (_ <- 0 until 5) {
      player = player.update(player.currentPosition + Position(1, 0), EmptyOwner, Direction(1, 0))
    }

    assert(player.trajectory.isEmpty)
  }

  "Player" should "have trajectory when moves outside territory" in {
    var player = Player(PlayerId(), Direction(1, 0), Position(), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
    for (_ <- 0 until 20) {
      player = player.update(player.currentPosition + Position(1, 0), EmptyOwner, Direction(1, 0))
    }

    assert(player.trajectory.nonEmpty)
  }

  "Player" should "be disabled when he crosses his own trajectory" in {
    var player = Player(PlayerId(), Direction(1, 0), Position(20, 20), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
      .copy(trajectory = Trajectory(Position(10, 0), Position(20, 0), Position(30, 0), Position(30, 30), Position(30, 20), Position(20, 19)))

    for (_ <- 0 until 11) {
      player = player.update(player.currentPosition + Position(1, 0), EmptyOwner, Direction(1, 0))
    }

    assert(!player.isActive)
  }

  "Player" should "be active if he didnt cross his own trajectory" in {
    var player = Player(PlayerId(), Direction(1, 0), Position(20, 20), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
      .copy(trajectory = Trajectory(Position(10, 0), Position(20, 0), Position(30, 0), Position(30, 30), Position(20, 30)))

    for (_ <- 0 until 9) {
      player = player.update(player.currentPosition + Position(1, 0), EmptyOwner, Direction(1, 0))
    }

    assert(player.isActive)
  }

  "Player" should "increase if he returned to territory" in {
    var player = Player(PlayerId(), Direction(0, -1), Position(0, 11), Territory(GeometryUtils.calculateCirclePoints(Position(), 10, 0.01F)))
      .copy(trajectory = Trajectory(Position(10, 0), Position(20, 0), Position(30, 0), Position(30, 30), Position(20, 30), Position(10, 30), Position(0, 30), Position(0, 11)))

    assert(player.territory.area() === 314.1F +- 0.1F)

    for (_ <- 0 until 2) {
      player = player.update(player.currentPosition + Position(0, -1), EmptyOwner, Direction(0, -1))
    }

    assert(player.territory.area() === 1135.8F +- 0.1F)
  }
}
