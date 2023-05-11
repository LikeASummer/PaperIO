package com.paper.io.geometry

import com.paper.io.geometry.base.{Degrees, Radians}
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.flatspec.AnyFlatSpec

class AngleFlatSpec extends AnyFlatSpec{
  "Radians" should "converted from Degrees" in {
    assert(Radians(Degrees(90)).value === 1.5708F +- 0.001F)
  }
}
