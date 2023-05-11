package com.paper.io.geometry.complex

import com.paper.io.geometry.base.Position
import com.paper.io.geometry.utils.GeometryUtils
import org.locationtech.jts.geom.{GeometryFactory, LineString}

final case class Radius(value: Float)

final case class Field(radius: Radius, diameter: Float, points: Vector[Position]) extends Shape[LineString] {
  val geometry: LineString = new GeometryFactory().createLineString(points.map(_.coordinate).toArray)

  def intersects(territory: Trajectory): Option[Position] = {
    val intersection = geometry.intersection(territory.geometry)
    Option.when(!intersection.isEmpty)(Position(intersection.getCoordinate))
  }

  def area(): Float = (Math.PI * Math.pow(radius.value, 2)).floatValue()
}

object Field {
  def apply(radius: Float): Field = apply(Radius(radius))
  def apply(radius: Radius): Field = new Field(radius, radius.value * 2, GeometryUtils.calculateCirclePoints(Position(), radius.value, 0.1f))

}
