package com.paper.io.geometry.base

case class Degrees(value: Float)

object Degrees {
  def apply(value: Float): Degrees = new Degrees(value)
}
