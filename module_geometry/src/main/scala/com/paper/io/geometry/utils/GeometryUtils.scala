package com.paper.io.geometry.utils

import com.paper.io.geometry.base.Position

import scala.collection.mutable.ListBuffer

object GeometryUtils {
  def calculateCirclePoints(position: Position, radius: Float, distanceBetweenPoints: Float): Vector[Position]= {
    var t = 0F
    val positions = ListBuffer[Position]()
    while (t < 2 * Math.PI) {
      positions.append(Position(radius * Math.cos(t).toFloat + position.x, radius * Math.sin(t).toFloat + position.y))
      t += distanceBetweenPoints
    }

    positions.toVector :+ positions.head
  }
}
