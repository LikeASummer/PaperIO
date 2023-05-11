package com.paper.io.geometry.base

final case class Direction(x: Float, y: Float) {
  def *(scalar: Float): Direction = {
    Direction(x * scalar, y * scalar)
  }

  def rotate(radians: Radians): Direction = {
    val newX = (Math.cos(radians.value) * x - Math.sin(radians.value) * y).toFloat
    val newY = (Math.sin(radians.value) * x + Math.cos(radians.value) * y).toFloat

    new Direction(newX, newY)
  }
}

object Direction{
  def apply(): Direction = Direction(0, 0)
  def apply(position: Position): Direction = Direction(position.x, position.y)
}
