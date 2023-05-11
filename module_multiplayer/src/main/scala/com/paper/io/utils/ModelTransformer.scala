package com.paper.io.utils

import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.Territory
import org.locationtech.jts.geom.Coordinate

import scala.language.implicitConversions
import scala.util.Try

object ModelTransformer {

  implicit def calculateAreaOfPoints(coordinates: Seq[com.paper.io.generated.Vector2D]): Float = Try(
    Territory(coordinates.map(vector => Position(vector.x, vector.y)).toVector).area()
  ).getOrElse(0)
}
