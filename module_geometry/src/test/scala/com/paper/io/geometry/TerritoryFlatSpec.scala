package com.paper.io.geometry

import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.{Territory, Trajectory}
import org.scalatest.flatspec.AnyFlatSpec

class TerritoryFlatSpec extends AnyFlatSpec {

  "Territory contains" should "return is point inside territory" in {
    val territory:Territory = Territory(Vector(Position(0, 0), Position(10, 0), Position(10, 10), Position(0, 10), Position(0, 0)))
    assert(territory.contains(Position(5, 5)))
    assert(territory.contains(Position(9, 5)))
    assert(!territory.contains(Position(11, 5)))
  }

  "Territory isEmpty/nonEmpty" should "if empty return True/False otherwise False/True" in {
    assert(Territory().isEmpty && !Territory().nonEmpty)
    val nonEmpty = Territory(Vector(Position(10, 9), Position(20, 19), Position(10, 9)))
    assert(!nonEmpty.isEmpty && nonEmpty.nonEmpty)
  }

  "Territory (SPLIT) :+" should "should create two territory candidate, when one contains trajectory points and other dont" in {
    val territory: Territory = Territory(Vector(Position(0, 0), Position(10, 0), Position(10, 10), Position(0, 10), Position(0, 0)))
    val trajectory: Trajectory = Trajectory(Position(0, 2), Position(5, 2), Position(10, 2))
    val segments = territory segment trajectory

    assert(segments.clockwise.area() == 80F)
  }

  "Territory (INCREASE RIGHT) :+" should "should create two territory candidate, when one contains trajectory points and other dont" in {
    val territory: Territory = Territory(Vector(Position(0, 0), Position(10, 0), Position(10, 10), Position(0, 10), Position(0, 0)))
    val trajectory: Trajectory = Trajectory(Position(10, 2), Position(20, 2), Position(20, 20), Position(10, 20), Position(10, 10))
    val segments = territory segment trajectory

    assert(segments.clockwise.area() == 320F)
  }

  "Territory (INCREASE LEFT) :+" should "should create two territory candidate, when one contains trajectory points and other dont" in {
    val territory: Territory = Territory(Vector(Position(10, 2), Position(20, 2), Position(20, 20), Position(10, 20), Position(10, 2)))
    val trajectory: Trajectory = Trajectory(Position(10, 2), Position(0, 2), Position(0, 10), Position(10, 10))
    val segments = territory segment trajectory

    assert(segments.counterclockwise.area() == 260)
  }
}
