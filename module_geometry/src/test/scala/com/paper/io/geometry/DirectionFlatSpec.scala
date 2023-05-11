package com.paper.io.geometry

import com.paper.io.geometry.base.{Degrees, Direction, Position, Radians}
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.flatspec.AnyFlatSpec

class DirectionFlatSpec extends AnyFlatSpec{
  "Direction" should "be multiplied by scalar value 8 from Direction(2, 3) to Direction(16, 24)" in {
    assert(Direction(2, 3) * 8 == Direction(16, 24))
  }

  it should "be rotated by 15 degrees" in {
    val direction = Direction(2, 3).rotate(Radians(Degrees(15)))
    assert(direction.x === 1.1F +- 0.06F)
    assert(direction.y === 3.4F +- 0.03F)
  }

  it should "created from position" in {
    val direction = Direction(Position(2, 2))
    assert(direction.x === 2)
    assert(direction.y === 2)
  }
}
