package com.paper.io

import com.paper.io.geometry.base.{Direction, Position}
import com.paper.io.geometry.complex.{Field, Territory, Trajectory}
import com.paper.io.geometry.special.EmptyOwner
import com.paper.io.geometry.utils.GeometryUtils
import com.paper.io.player.{Disabled, Player, PlayerId}


class Game private (val field: Field,
                    val players: Map[PlayerId, Player],
                    val state: State,
                    val startTerritory: Float,
                    val numberOfPlayers: Int) {

  def startGame(): Game = {
    new Game(field, players, InProgress, startTerritory, numberOfPlayers)
  }

  def endGame(): Game = {
    new Game(field, players, Ended, startTerritory, numberOfPlayers)
  }

  def update(id: PlayerId, nextPosition: Position, nextDirection: Direction): Game = {
    (players.get(id), state) match {
      case (Some(player), InProgress) =>
        val ownerOfNextPosition = players.values
          .find(otherPlayer => otherPlayer.territory.contains(nextPosition))
          .map(_.id)
          .getOrElse(EmptyOwner)

        val updatedPlayer = if (nextPosition.distance(Position()) >= field.radius.value)
          player.copy(currentState = Disabled)
        else
          player.update(nextPosition, ownerOfNextPosition, nextDirection)

        val disabledPlayers = disableCrossedPlayers(updatedPlayer, player.territory, players)
        val playersWithReducedTerritory = claimTerritoryTakenFromOtherPlayers(updatedPlayer, player.trajectory, disabledPlayers)
        new Game(field, playersWithReducedTerritory + (id -> updatedPlayer), state, startTerritory, numberOfPlayers)
      case _ => new Game(field, players, state, startTerritory, numberOfPlayers)
    }
  }

  def addPlayer(id: PlayerId): Game = {
    if (state == WaitingForPlayers) {
      val random = new scala.util.Random()
      var position = Position()

      do {
        val angle = Math.random() * Math.PI * 2
        val randomRadius = random.between(0, field.radius.value - startTerritory)
        position = Position((Math.cos(angle) * randomRadius).toFloat, (Math.sin(angle) * randomRadius).toFloat)
      } while (!isPositionFree(position))

      val direction = Direction(Math.random().toFloat * (1 + 1) - 1, Math.random().toFloat * (1 + 1) - 1)
      val territory = Territory(GeometryUtils.calculateCirclePoints(position, startTerritory, 0.1F))
      val player = Player(id, direction, position, territory)
      new Game(field, players + (id -> player), state, startTerritory, numberOfPlayers)
    } else {
      new Game(field, players, state, startTerritory, numberOfPlayers)
    }
  }

  def disablePlayer(id: PlayerId): Game = {
    val updatedPlayers = players.get(id) match {
      case Some(player) => players + (id -> player.copy(currentState = Disabled))
      case _ => players
    }

    new Game(field, updatedPlayers, state, startTerritory, numberOfPlayers)
  }

  private def isPositionFree(position: Position): Boolean = {
    players.values.forall(player => player.currentPosition.distance(position) > startTerritory * 2)
  }

  private def claimTerritoryTakenFromOtherPlayers(player: Player, trajectory: Trajectory, originalPlayer: Map[PlayerId, Player]): Map[PlayerId, Player] = {
    var players = originalPlayer

    reduceOtherPlayerTerritory(player, trajectory)
      .filter { case (id, _) => players.contains(id)}
      .foreach { case (id, reductions) =>
        players = players + (id -> players(id).reduce(reductions))
      }

    players
  }


  private def disableCrossedPlayers(player: Player, oldTerritory: Territory, originalPlayer: Map[PlayerId, Player]): Map[PlayerId, Player] = {
    var players = originalPlayer

    val headTrajectory = Trajectory(player.currentPosition, player.previousPosition)

    for (otherPlayer <- players.values) {
      if (otherPlayer.id != player.id) {
        val playerCrossesOtherPlayerTrajectory = otherPlayer.trajectory.nonEmpty && headTrajectory.intersects(otherPlayer.trajectory)
        val otherPlayerIsInsideNewTerritory = player.territory.contains(otherPlayer.currentPosition) || player.territory.contains(otherPlayer.lastPositionOnTerritory)
        val otherPlayerNotInsideOldTerritory = !oldTerritory.contains(otherPlayer.currentPosition)
        val playerConsumedOtherPlayer = otherPlayerIsInsideNewTerritory && otherPlayerNotInsideOldTerritory

        if (playerCrossesOtherPlayerTrajectory || playerConsumedOtherPlayer) {
          players = players + (otherPlayer.id -> otherPlayer.copy(currentState = Disabled))
        }
      }
    }

    players
  }

  private def reduceOtherPlayerTerritory(player: Player, previousTrajectory: Trajectory): Map[PlayerId, List[Trajectory]] = {
    if (player.territory.contains(player.currentPosition) && previousTrajectory.nonEmpty) {
      previousTrajectory.segment()
        .filter(segment => segment.nonEmpty)
        .filter(segment => segment.points.head.owner.isInstanceOf[PlayerId])
        .groupBy(_.points.head.owner.asInstanceOf[PlayerId])
    } else {
      Map()
    }
  }
}

object Game {
  def apply(fieldRadius: Float, startTerritory: Float, numberOfPlayers: Int): Game = {
    new Game(Field(fieldRadius), Map.empty, WaitingForPlayers, startTerritory, numberOfPlayers)
  }
}
