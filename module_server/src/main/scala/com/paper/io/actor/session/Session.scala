package com.paper.io.actor.session

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.stream.scaladsl.{Sink, Source}
import com.paper.io.{Game, InProgress}
import com.paper.io.actor.robot.Robot
import com.paper.io.actor.robot.Robot.{RobotMessage, Update}
import com.paper.io.domain.ConnectionId
import com.paper.io.generated.{PlayerInput, SessionMetadata, SessionState}
import com.paper.io.ml.Model
import com.paper.io.player.PlayerId
import com.paper.io.domain.ModelTransformers.{gameStateToSessionState, gameToMetadata, fieldToCoordinates}
import com.paper.io.geometry.base.{Direction, Position}

import scala.concurrent.duration.DurationInt

object Session {

  trait SessionMessage
  final case class Connect(connectionId: ConnectionId, outputSink: akka.actor.ActorRef, inputSource: Source[PlayerInput, NotUsed]) extends SessionMessage
  final case class Disconnect(connectionId: ConnectionId) extends SessionMessage
  final case class InternalUpdate(position: Position, direction: Direction, playerId: PlayerId) extends SessionMessage
  private final case class ExternalUpdate(playerInput: PlayerInput, connectionId: ConnectionId) extends SessionMessage

  private final case object InternalClock extends SessionMessage
  private final case object ExternalClock extends SessionMessage
  private final case object Start extends SessionMessage
  private final case object End extends SessionMessage

  def create(): Behavior[SessionMessage] = Behaviors.withTimers { context => {
    context.startTimerAtFixedRate(InternalClock, 33.milliseconds)
    context.startTimerAtFixedRate(ExternalClock, 33.milliseconds)

    context.startSingleTimer(Start, 10.seconds)
    context.startSingleTimer(End, 3.minute)

    change(Map.empty, Map.empty, Map.empty, Game(100, 20, 8))
  }}

  private def change(connectionToPlayer: Map[ConnectionId, PlayerId],
            connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
            connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
            game: Game): Behavior[SessionMessage] = Behaviors.setup { _ => {
    Behaviors.receive[SessionMessage] { case (context, message) =>
      message match {
        case connect: Connect =>
          context.log.info(s"Connecting player to session")
          connectPlayer(connect, connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
        case disconnect: Disconnect =>
          context.log.info(s"Disconnecting player from session")
          disconnectPlayer(disconnect, connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
        case ExternalUpdate(playerInput, connectionId) =>
          change(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, updatePlayer(playerInput, game, connectionId, connectionToPlayer))
        case InternalUpdate(position, direction, playerId) =>
          change(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game.update(playerId, position, direction))
        case Start =>
          context.log.info(s"Start game called")
          startGame(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
        case End =>
          context.log.info(s"Ended game called")
          endGame(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
        case ExternalClock =>
          checkAndNotifyPlayers(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
        case InternalClock =>
          checkAndNotifyArtificialPlayer(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
      }
    }
  }}

  private def connectPlayer(connect: Connect,
                            connectionToPlayer: Map[ConnectionId, PlayerId],
                            connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                            connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                            game: Game, context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {
    val playerId = PlayerId()
    val newGame = game.addPlayer(playerId)

    processPlayerInput(connect.inputSource, context, connect.connectionId)(context.system)

    val newConnectionToOutput = connectionToOutput + (connect.connectionId -> connect.outputSink)
    val newConnectionToPlayer = connectionToPlayer + (connect.connectionId -> playerId)

    notifyPlayer(connect.outputSink, playerId, game)
    change(newConnectionToPlayer, newConnectionToOutput, connectionToArtificialPlayers, newGame)
  }

  private def disconnectPlayer(disconnect: Disconnect,
                               connectionToPlayer: Map[ConnectionId, PlayerId],
                               connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                               connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                               game: Game,
                               context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {
    var newGame = game
    for (playerId <- connectionToPlayer.get(disconnect.connectionId)) {
      newGame = game.disablePlayer(playerId)

      for (ref <- connectionToOutput.get(disconnect.connectionId)) {
        closeConnectionWithPlayer(ref, playerId, game)
      }
    }

    val newConnectionToPlayer = connectionToPlayer.filterNot {
      case (id, _) => id == disconnect.connectionId
    }

    val newConnectionToOutput = connectionToOutput.filterNot {
      case (id, _) => id == disconnect.connectionId
    }

    if (newConnectionToPlayer.isEmpty) {
      endGame(newConnectionToPlayer, newConnectionToOutput, connectionToArtificialPlayers, game, context)
    } else {
      change(newConnectionToPlayer, newConnectionToOutput, connectionToArtificialPlayers, newGame)
    }
  }

  private def startGame(connectionToPlayer: Map[ConnectionId, PlayerId],
                        connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                        connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                        game: Game,
                        context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {

    context.log.info("Starting game")

    val numberOfConnectedPlayers = connectionToPlayer.size
    val missingPlayerCount = game.numberOfPlayers - numberOfConnectedPlayers
    context.log.info(s"$missingPlayerCount - Artificial Players should be added")

    var newConnectionToArtificialPlayers = connectionToArtificialPlayers

    var newGame = game
    if (missingPlayerCount > 0) {
      for (model <- Model("models/model.json")) {
        for (i <- 0 until missingPlayerCount) {
          context.log.info(s"Creating Artificial Player - $i")

          val playerId = PlayerId()
          newGame = newGame.addPlayer(playerId)
          val robotActor = context.spawn(Robot(context.self, playerId, model.copy(), game.field), s"robot_$playerId")
          newConnectionToArtificialPlayers = newConnectionToArtificialPlayers + (playerId -> robotActor)
        }
      }
    }

    change(connectionToPlayer, connectionToOutput, newConnectionToArtificialPlayers, newGame.startGame())
  }

  private def endGame(connectionToPlayer: Map[ConnectionId, PlayerId],
                      connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                      connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                      game: Game,
                      context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {
    context.log.info("Ending game")

    val newGame = game.endGame()
    connectionToOutput.foreach{case (connectionId: ConnectionId, ref) =>
      for (playerId <- connectionToPlayer.get(connectionId)){
        closeConnectionWithPlayer(ref, playerId, newGame)
      }
    }

    connectionToArtificialPlayers.foreach{case (_, ref)  => context.stop(ref)}
    Behaviors.stopped
  }

  private def updatePlayer(playerInput: PlayerInput, game: Game, connectionId: ConnectionId, connectionToPlayer: Map[ConnectionId, PlayerId]) : Game = {
    val newGame = for {
      playerId <- connectionToPlayer.get(connectionId)
      nextPosition <- playerInput.position
      nextDirection <- playerInput.direction
      result <- Some(game.update(playerId, Position (nextPosition.x, nextPosition.y), Direction (nextDirection.x, nextDirection.y) ) )
    } yield result

    newGame.getOrElse(game)
  }

  private def processPlayerInput(inputSource: Source[PlayerInput, NotUsed], context: ActorContext[SessionMessage], connectionId: ConnectionId)(implicit system: ActorSystem[Nothing]): Unit = {
    inputSource.runWith(Sink.foreach(input => context.self ! ExternalUpdate(input, connectionId)))
  }

  private def checkAndNotifyPlayers(connectionToPlayer: Map[ConnectionId, PlayerId],
                                    connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                                    connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                                    game: Game,
                                    context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {

    if(game.state != InProgress) {
      return Behaviors.same
    }

    var newConnectionToPlayer = connectionToPlayer
    var newConnectionToOutput = connectionToOutput

    connectionToPlayer.toSeq.filter { case (_, playerId) =>
      game.players.get(playerId).exists(player => !player.isActive)
    }.foreach { case (connectionId, playerId) =>
      for (ref <- connectionToOutput.get(connectionId)) {
        closeConnectionWithPlayer(ref, playerId, game)
      }

      newConnectionToOutput = newConnectionToOutput - connectionId
      newConnectionToPlayer = newConnectionToPlayer - connectionId
    }

    newConnectionToOutput.foreach { case (connectionId, ref) => notifyPlayer(ref, connectionToPlayer(connectionId), game) }
    completeOrContinue(newConnectionToPlayer, newConnectionToOutput, connectionToArtificialPlayers, game, context)
  }

  private def notifyPlayer(ref: akka.actor.ActorRef, playerId: PlayerId, game: Game): Unit = {
    ref ! new SessionMetadata(
      playerId = playerId.value.toString,
      state = game.state,
      players = game.players.values,
      field = game.field
    )
  }

  private def checkAndNotifyArtificialPlayer(connectionToPlayer: Map[ConnectionId, PlayerId],
                                             connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                                             connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                                             game: Game,
                                             context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {
    if (game.state != InProgress) {
      return Behaviors.same
    }

    var newConnectionToArtificialPlayers = connectionToArtificialPlayers
    connectionToArtificialPlayers.filter {case (playerId, _) =>
      game.players.get (playerId).exists (player => ! player.isActive)
    }.foreach {case (playerId, ref) =>
      context.stop(ref)
      newConnectionToArtificialPlayers = newConnectionToArtificialPlayers - playerId
    }

    newConnectionToArtificialPlayers.foreach {case (_, ref) => notifyArtificialPlayer (ref, game)}
    completeOrContinue(connectionToPlayer, connectionToOutput, newConnectionToArtificialPlayers, game, context)
  }

  private def notifyArtificialPlayer(ref: ActorRef[RobotMessage], game: Game): Unit = {
    ref ! Update(game)
  }

  private def completeOrContinue(connectionToPlayer: Map[ConnectionId, PlayerId],
                                 connectionToOutput: Map[ConnectionId, akka.actor.ActorRef],
                                 connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]],
                                 game: Game,
                                 context: ActorContext[SessionMessage]): Behavior[SessionMessage] = {

    if (completionTrigger(connectionToPlayer, connectionToArtificialPlayers)) {
      endGame(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game, context)
    } else {
      change(connectionToPlayer, connectionToOutput, connectionToArtificialPlayers, game)
    }
  }

  private def completionTrigger(connectionToPlayer: Map[ConnectionId, PlayerId], connectionToArtificialPlayers: Map[PlayerId, ActorRef[RobotMessage]]): Boolean = {
    connectionToPlayer.isEmpty || (connectionToPlayer.size == 1 && connectionToArtificialPlayers.isEmpty)
  }

  private def closeConnectionWithPlayer(ref: akka.actor.ActorRef, playerId: PlayerId, game: Game): Unit = {
    ref ! new SessionMetadata(playerId = playerId.value.toString, state = SessionState.ENDED, players = game.players.values, field = game.field)
    ref ! new SessionMetadata(playerId = playerId.value.toString, state = SessionState.CLOSE_CONNECTION)
  }
}
