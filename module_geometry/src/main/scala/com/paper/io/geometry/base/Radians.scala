package com.paper.io.geometry.base

case class Radians(value: Float) {
  def *(scalar: Float): Radians = Radians(value * scalar)
}

object Radians {
  def apply(radians: Float): Radians = new Radians(radians)
  def apply(degrees: Degrees): Radians = new Radians((degrees.value * Math.PI / 180).toFloat)
}