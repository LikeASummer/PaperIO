package com.paper.io.player

import scala.util.Random

case class Color(red: Float, green: Float, blue: Float)

object Color {
  def apply(): Color = {
    val random = new Random()
    new Color(random.nextDouble().toFloat, random.nextDouble().toFloat, random.nextDouble().toFloat)
  }
}
