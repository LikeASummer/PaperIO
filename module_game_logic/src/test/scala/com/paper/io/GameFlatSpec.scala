package com.paper.io

import com.paper.io.geometry.base.{Direction, Position}
import com.paper.io.player.PlayerId
import org.scalatest.flatspec.AnyFlatSpec

class GameFlatSpec extends AnyFlatSpec {
  "Game" should "be created with WaitingForPlayers state" in {
    val game = Game(100, 10, 8)
    assert(game.state == WaitingForPlayers)
  }

  "Game" should "be change state to InProgress state when game started" in {
    val game = Game(100, 10, 8).startGame()
    assert(game.state == InProgress)
  }

  "Game" should "be change state to Ended state when game started" in {
    val game = Game(100, 10, 8).endGame()
    assert(game.state == Ended)
  }

  "Game" should "be register player only in WaitingForPlayers state" in {
    var game = Game(100, 10, 8)
    game = game.addPlayer(PlayerId())
    game = game.addPlayer(PlayerId())
    assert(game.players.size == 2)

    game = game.startGame()

    game = game.addPlayer(PlayerId())
    assert(game.players.size == 2)
  }

  "Game" should "disable player with disablePlayer" in {
    var game = Game(100, 10, 8)
    val playerId = PlayerId()
    game = game.addPlayer(playerId)
    assert(game.players(playerId).isActive)
    game = game.disablePlayer(playerId)
    assert(!game.players(playerId).isActive)
  }

  "Game" should "move player with update" in {
    var game = Game(100, 10, 8)
    val playerId = PlayerId()
    game = game.addPlayer(playerId)

    val currentPlayerPosition = game.players(playerId).currentPosition
    game = game.startGame()
    game = game.update(playerId, currentPlayerPosition + Position(0, 0.5F), Direction(0, 1))

    assert(game.players(playerId).currentPosition == currentPlayerPosition + Position(0, 0.5F))
  }
}
