package com.paper.io.geometry.base

import org.locationtech.jts.geom.Coordinate

import scala.language.implicitConversions

final case class Position(x: Float, y: Float) {
  val coordinate: Coordinate = new Coordinate(x, y)
  def distance(other: Position): Float = coordinate.distance(other.coordinate).toFloat
  def distanceNormalized(other: Position, totalDistance: Float): Float = coordinate.distance(other.coordinate).toFloat / totalDistance
  def +(direction: Direction): Position = Position(x + direction.x, y + direction.y)
  def -(position: Position): Position = Position(x - position.x, y - position.y)
  def +(position: Position): Position = Position(x + position.x, y + position.y)
}

object Position {
  def apply(): Position = new Position(0, 0)
  def apply(x: Float, y: Float): Position = new Position(x, y)
  def apply(coordinate: Coordinate): Position = new Position(coordinate.x.toFloat, coordinate.y.toFloat)

  implicit def directionToPosition(direction: Direction): Position = new Position(direction.x, direction.y)
}

