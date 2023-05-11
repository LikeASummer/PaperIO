package com.paper.io.domain

import com.paper.io.{Ended, InProgress, WaitingForPlayers}
import com.paper.io.generated.{PlayerColor, PlayerState, SessionState, Vector2D}
import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.{Field, Territory, Trajectory}
import com.paper.io.player.{Color, Player}

import scala.language.implicitConversions

object ModelTransformers {
  implicit def trajectoryToCoordinates(trajectory: Trajectory): Seq[Vector2D] = trajectory.points
    .map(position => Vector2D(x = position.position.x, y = position.position.y))

  implicit def territoryToCoordinates(territory: Territory): Seq[Vector2D] = territory.points
    .map(position => Vector2D(x = position.x, y = position.y))

  implicit def fieldToCoordinates(field: Field): Seq[Vector2D] = field.points
    .map(position => Vector2D(x = position.x, y = position.y))

  implicit def positionToCoordinate(position: Position): Vector2D = Vector2D(x = position.x, y = position.y)

  implicit def colorToPlayerColor(color: Color): PlayerColor = PlayerColor(color.red, color.green, color.blue)

  implicit def gameStateToSessionState(gameState: com.paper.io.State): SessionState = gameState match {
    case WaitingForPlayers => SessionState.WAITING_FOR_PLAYERS
    case InProgress => SessionState.IN_PROGRESS
    case Ended => SessionState.ENDED
  }

  implicit def playerToMetadata(player: Player): PlayerState = {
    PlayerState(
      player.id.toString,
      Some(player.currentPosition),
      player.trajectory,
      player.territory,
      player.isActive,
      Some(player.color)
    )
  }

  implicit def gameToMetadata(players: Iterable[Player]): Seq[PlayerState] = {
    players.map(playerToMetadata).toSeq
  }
}
