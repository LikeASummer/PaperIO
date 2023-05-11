package com.paper.io.ml

import com.paper.io.geometry.base.{Direction, Position}

case class Ray(start: Position, end: Position)

object Ray {
  def apply(start: Position, direction: Direction, distance: Float): Ray = {
    new Ray(start, direction * distance)
  }
}
