package com.paper.io.geometry

import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.Trajectory
import com.paper.io.geometry.special.{EmptyOwner, OwnedPosition, Owner}
import org.scalactic.Tolerance.convertNumericToPlusOrMinusWrapper
import org.scalatest.flatspec.AnyFlatSpec

case object TestOwnerOne extends Owner
case object TestOwnerTwo extends Owner

class TrajectoryFlatSpec extends AnyFlatSpec{
  "Trajectory length" should "be calculated correctly (0,0) -> (10,10) -> (45,78) = 90.62" in {
    val trajectory = Trajectory() :+ Position(0, 0) :+ Position(10, 10) :+ Position(45, 78)
    assert(trajectory.length === 90.62F +- 0.01F)
  }

  "Trajectory isEmpty/nonEmpty" should "if empty return True/False otherwise False/True" in {
    assert(Trajectory().isEmpty && !Trajectory().nonEmpty)
    val nonEmpty = Trajectory(Position(10, 9), Position(20, 19))
    assert(!nonEmpty.isEmpty && nonEmpty.nonEmpty)
  }

  "Trajectory :+" should "add new position to the end of trajectory" in {
    val trajectory = Trajectory(Position(0, 0), Position(0, 0)) :+ Position(10, 10)
    assert(trajectory.points.last.position === Position(10, 10))
  }

  "Trajectory segment" should "split trajectory to vector of trajectories aggregated by owner" in {
    val trajectory= Trajectory(
      OwnedPosition(Position(0, 0), EmptyOwner),
      OwnedPosition(Position(10, 10), TestOwnerOne),
      OwnedPosition(Position(45, 78), TestOwnerOne),
      OwnedPosition(Position(75, 78), TestOwnerTwo),
      OwnedPosition(Position(100, 100), TestOwnerOne)
    )
    assert(trajectory.segment().length == 4)
  }
}
