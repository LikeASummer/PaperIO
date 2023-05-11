package com.paper.io.geometry.complex

import com.paper.io.geometry.base.Position
import org.locationtech.jts.geom.{GeometryFactory, Polygon}

import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}

case class TerritorySegments(clockwise: Territory, counterclockwise: Territory)

final class Territory private (val points: Vector[Position]) extends ClosedShape[Polygon] {
  val geometry: Polygon = new GeometryFactory().createPolygon(points.map(_.coordinate).toArray)

  def segment(trajectory: Trajectory): TerritorySegments = {
    val line = trajectory.points.map(_.position)
    val (_, startIndex) = points.zipWithIndex.minBy { case (point, _) => point.distance(line.head) }
    val (_, endIndex) = points.zipWithIndex.minBy { case (point, _) => point.distance(line.last) }

    val clockwiseTerritory = calculateClockwiseTerritory(startIndex, endIndex, points, line)
    val counterclockwiseTerritory = calculateCounterclockwiseTerritory(startIndex, endIndex, points, line)

    TerritorySegments(clockwiseTerritory, counterclockwiseTerritory)
  }

  def contains(point: Position): Boolean = {
    geometry.contains(new GeometryFactory().createPoint(point.coordinate))
  }

  def isEmpty: Boolean = points.isEmpty
  def nonEmpty: Boolean = points.nonEmpty

  private def calculateClockwiseRedundantVertices(startIndex: Int, endIndex: Int, points: Vector[Position]): List[Position] = {
    val redundantVertices = mutable.ListBuffer[Position]()

    var i = startIndex
    breakable {
      while (i != endIndex) {
        if (i == points.length) {
          if (endIndex == 0) {
            break
          }
          i = 0
        }

        redundantVertices.addOne(points(i))
        i += 1
      }
    }
    redundantVertices.addOne(points(endIndex))
    redundantVertices.toList
  }

  private def calculateClockwiseTerritory(startIndex: Int, endIndex: Int, points: Vector[Position], line: Vector[Position]): Territory = {
    val redundantVertices = calculateClockwiseRedundantVertices(startIndex, endIndex, points)

    val tempAreaClockWise = mutable.ListBuffer[Position]()
    tempAreaClockWise.addAll(points)

    for (i <- line.indices) {
      tempAreaClockWise.insert(i + startIndex, line(i))
    }

    val withoutRedundant = filterRedundant(tempAreaClockWise.toList, redundantVertices)
    Territory(withoutRedundant.toVector :+ withoutRedundant.head)
  }

  private def calculateCounterclockwiseRedundantVertices(startIndex: Int, endIndex: Int, points: Vector[Position]): List[Position] = {
    val redundantVertices = mutable.ListBuffer[Position]()
    var j = startIndex
    breakable {
      while (j != endIndex) {
        if (j == -1) {
          if (endIndex == points.length - 1) {
            break
          }
          j = points.length - 1
        }

        redundantVertices.addOne(points(j))
        j -= 1
      }
    }
    redundantVertices.addOne(points(endIndex))
    redundantVertices.toList
  }

  private def calculateCounterclockwiseTerritory(startIndex: Int, endIndex: Int, points: Vector[Position], line: Vector[Position]): Territory = {
    val redundantVertices = calculateCounterclockwiseRedundantVertices(startIndex, endIndex, points)

    var tempAreaCounterclockwise = mutable.ListBuffer[Position]()
    tempAreaCounterclockwise.addAll(points)

    for (i <- line.indices) {
      tempAreaCounterclockwise.insert(startIndex, line(i))
    }

    tempAreaCounterclockwise = tempAreaCounterclockwise.filter(point => !redundantVertices.contains(point))
    Territory(tempAreaCounterclockwise.toVector :+ tempAreaCounterclockwise.head)
  }

  private def filterRedundant(data: List[Position], redundant: List[Position]): List[Position] = {
    data.filter(point => !redundant.contains(point))
  }
}

object Territory {
  def apply(): Territory = new Territory(Vector())

  def apply(points: Vector[Position]): Territory = points match {
    case points if points.head == points.last => new Territory(Vector.from(points))
    case points if points.head != points.last => new Territory(Vector.from(points) :+ points.head)
    case _ => Territory()
  }

  def apply(line: Trajectory): Option[Territory] = line match {
    case Trajectory(points) if points.nonEmpty => Option(new Territory(points.map(_.position) :+ points.head.position))
    case _ => Option.empty
  }
}
