package com.paper.io.geometry

import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.{Field, Trajectory}
import org.scalatest.flatspec.AnyFlatSpec

class FieldFlatSpec extends AnyFlatSpec{
  "Field intersects" should "return true if trajectory intersects it" in {
    val field = Field(100)

    assert(field.intersects(Trajectory(Position(), Position(0, 101))).isDefined)
    assert(field.intersects(Trajectory(Position(), Position(0, 99))).isEmpty)
  }
}
