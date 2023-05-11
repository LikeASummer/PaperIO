package com.paper.io.geometry

import com.paper.io.geometry.base.{Direction, Position}
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.flatspec.AnyFlatSpec

class PositionFlatSpec extends AnyFlatSpec{
  "Position" should "calculate correct distance" in {
    assert((Position(20, 0) distance Position(10, 0)) == 10F)
    assert((Position(0, 10) distance Position(0, 20)) == 10F)
    assert((Position(15, 15) distance Position(10, 20)) === 7F +- 0.1F)
  }

  it should "be added with Position" in {
    assert((Position(10, 10) + Position(10, 0)) === Position(20, 10))
    assert((Position(10, 10) + Position(0, 10)) === Position(10, 20))
    assert((Position(10, 10) + Position(10, 10)) === Position(20, 20))
  }

  it should "be subtracted with Position" in {
    assert((Position(10, 10) - Position(10, 0)) === Position(0, 10))
    assert((Position(10, 10) - Position(0, 10)) === Position(10, 0))
    assert((Position(10, 10) - Position(10, 20)) === Position(0, -10))
  }

  it should "be added with Direction" in {
    assert((Position(10, 10) + Direction(10, 0)) === Position(20, 10))
    assert((Position(10, 10) + Direction(0, 10)) === Position(10, 20))
    assert((Position(10, 10) + Direction(10, 10)) === Position(20, 20))
  }
}
