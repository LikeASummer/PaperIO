package com.paper.io.geometry.complex

import com.paper.io.geometry.special.OwnedPosition
import org.locationtech.jts.geom.{GeometryFactory, LineString}

import scala.collection.mutable

final case class Trajectory(points: Vector[OwnedPosition]) extends Shape[LineString] {
  val geometry: LineString = new GeometryFactory().createLineString(points.map(_.position.coordinate).toArray)

  def :+(point: OwnedPosition): Trajectory = {
    points match {
      case Vector() => Trajectory(points :+ point :+ point)
      case _ => Trajectory(points :+ point)
    }
  }

  def segment(): List[Trajectory] = {
    points.foldLeft(mutable.ListBuffer[Trajectory]()) {
      case (accumulator, position) =>
        if (accumulator.isEmpty || accumulator.last.points.head.owner != position.owner) {
          val trajectory = Trajectory(position, position)
          accumulator.addOne(trajectory)
        } else {
          val trajectory = accumulator.last :+ position
          accumulator.remove(accumulator.length - 1)
          accumulator.addOne(trajectory)
        }
    }.toList
  }

  def length: Float = if (nonEmpty) geometry.getLength.toFloat else 0F
  def isEmpty: Boolean = points.isEmpty
  def nonEmpty: Boolean = points.nonEmpty
}

object Trajectory {
  def apply(points: OwnedPosition*): Trajectory = new Trajectory(Vector.from(points))
  def apply(): Trajectory = new Trajectory(Vector())
}