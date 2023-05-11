package com.paper.io.player

import com.paper.io.geometry.base.{Direction, Position}
import com.paper.io.geometry.complex.{Territory, Trajectory}
import com.paper.io.geometry.special.{OwnedPosition, Owner}

final case class Player(id: PlayerId,
                        direction: Direction,
                        territory: Territory,
                        trajectory: Trajectory,
                        lastPositionOnTerritory: Position,
                        currentPosition: Position,
                        previousPosition: Position,
                        currentState: PlayerState,
                        color: Color = Color()) {

  def isActive: Boolean = currentState == Active
  def canEqual(other: Any): Boolean = other.isInstanceOf[Player]

  def update(nextPosition: Position,
                     nextPositionOwner: Owner,
                     nextDirection: Direction): Player = {
    val nextState = calculateState(nextPosition)
    val nextTerritory = calculateTerritory(nextPosition)
    val nextTrajectory = calculateTrajectory(nextPosition, nextPositionOwner)
    val nextLastPositionOnTerritory = calculateLastPositionOnTerritory(nextTerritory, nextPosition, lastPositionOnTerritory)

    copy(
      direction = nextDirection,
      territory = nextTerritory,
      trajectory = nextTrajectory,
      currentPosition = nextPosition,
      currentState = nextState,
      previousPosition = currentPosition,
      lastPositionOnTerritory = nextLastPositionOnTerritory
    )
  }

  def reduce(reductions : List[Trajectory]): Player = {
    val reducedTerritory = reductions.foldLeft(territory) {
        case (territory, trajectory) if trajectory.points.exists(p => territory.contains(p.position)) =>
          val territorySegments = territory.segment(trajectory)
          if (territorySegments.clockwise.contains(lastPositionOnTerritory)) territorySegments.clockwise else territorySegments.counterclockwise
        case (territory, _) => territory
      }

    copy(territory = reducedTerritory)
  }

  private def calculateState(nextPosition: Position): PlayerState = {
    val headTrajectory = Trajectory(nextPosition, currentPosition)
    val isPlayerCrossedHisOwnTrajectory = headTrajectory.intersects(trajectory)

    val wasPlayerDisabled = isPlayerCrossedHisOwnTrajectory || !isActive
    if (wasPlayerDisabled) Disabled else currentState
  }

  private def calculateTerritory(nextPosition: Position): Territory = {
    val isPlayerOutsideTerritory = !territory.contains(nextPosition)
    val wasPlayerOutsideTerritoryBefore = !trajectory.isEmpty

    if (!isPlayerOutsideTerritory && wasPlayerOutsideTerritoryBefore) {
      val territorySegments = territory segment trajectory
      if (territorySegments.clockwise.area() > territorySegments.counterclockwise.area()) territorySegments.clockwise else territorySegments.counterclockwise
    } else territory
  }

  private def calculateTrajectory(nextPosition: Position, nextPositionOwner: Owner): Trajectory = {
    (!territory.contains(nextPosition), !trajectory.isEmpty) match {
      case (true, true) => trajectory :+ OwnedPosition(currentPosition, nextPositionOwner)
      case (true, false) => Trajectory(lastPositionOnTerritory, OwnedPosition(currentPosition, nextPositionOwner))
      case _ => Trajectory()
    }
  }

  private def calculateLastPositionOnTerritory(nextTerritory: Territory, nextPosition: Position, lastPositionInTerritory: Position): Position = {
    if (nextTerritory.contains(nextPosition)) nextPosition else lastPositionInTerritory
  }

  override def equals(other: Any): Boolean = other match {
    case that: Player =>
      (that canEqual this) &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(id)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Player {
  def apply(id: PlayerId, direction: Direction, position: Position, territory: Territory): Player =
    new Player(id, direction, territory, Trajectory(), position, position, position, Active)
}
