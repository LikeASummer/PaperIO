package com.paper.io.geometry.complex

import org.locationtech.jts.geom.Geometry

trait Shape[T <: Geometry] {
  val geometry: T

  def intersects(shape: Shape[T]): Boolean = geometry.intersects(shape.geometry)
}

trait ClosedShape[T <: Geometry] extends Shape[T] {
  def area(): Float = geometry.getArea.toFloat
}