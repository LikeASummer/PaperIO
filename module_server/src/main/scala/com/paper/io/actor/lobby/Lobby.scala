package com.paper.io.actor.lobby

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.paper.io.domain.{ConnectionId, SessionId}
import com.paper.io.generated.{LobbyResponse, LobbyStatus}

import java.time.{Duration, LocalDateTime}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AwaitingPlayer(connectionId: ConnectionId, outStream: ActorRef, connected: LocalDateTime)

object Lobby {

  trait LobbyManagerMessage
  final case class ConnectToLobby(connectionId: ConnectionId, outputSink: ActorRef) extends LobbyManagerMessage
  final case class DisconnectFromLobby(connectionId: ConnectionId) extends LobbyManagerMessage
  private final case object Clock extends LobbyManagerMessage

  def create()(implicit usersPerSession: Int, awaitingTimeLimit: FiniteDuration): Behavior[LobbyManagerMessage] = Behaviors.withTimers { context =>
    context.startTimerAtFixedRate(Clock, 10.seconds)
    change(Map.empty)
  }

  private def change(awaitingPlayers: Map[ConnectionId, AwaitingPlayer])(implicit usersPerSession: Int, awaitingTimeLimit: FiniteDuration): Behavior[LobbyManagerMessage] = Behaviors.setup { _ =>
    Behaviors.receiveMessage[LobbyManagerMessage] {
      case ConnectToLobby(connectionId, outStream) =>
        notifyOfWaiting(outStream)
        change(awaitingPlayers + (connectionId -> AwaitingPlayer(connectionId, outStream, LocalDateTime.now())))
      case DisconnectFromLobby(connectionId) => change(awaitingPlayers - connectionId)
      case Clock =>
        var processedPlayers = awaitingPlayers
        processedPlayers.values.foreach(player => notifyOfWaiting(player.outStream))

        if (processedPlayers.size / usersPerSession > 0) {
          processedPlayers.values
            .sliding(usersPerSession)
            .foreach(sessionPlayers => processedPlayers = createSession(sessionPlayers, processedPlayers))
        }

        val currentDateTime = LocalDateTime.now()
        val playersWhoWaitedTooLong = processedPlayers.values
          .exists(player => Duration.between(player.connected, currentDateTime).getSeconds >= awaitingTimeLimit.toSeconds)

        if (playersWhoWaitedTooLong){
          processedPlayers = createSession(processedPlayers.values, processedPlayers)
        }

        change(processedPlayers)
    }
  }

  private def createSession(sessionPlayers: Iterable[AwaitingPlayer], players: Map[ConnectionId, AwaitingPlayer]): Map[ConnectionId, AwaitingPlayer] = {
    var processedPlayers = players

    val sessionId = SessionId()
    sessionPlayers.foreach(player => {
      processedPlayers = processedPlayers - player.connectionId
      notifyOfCreated(player.outStream, sessionId)
    })

    processedPlayers
  }
  private def notifyOfWaiting(ref: ActorRef): Unit = ref ! new LobbyResponse(LobbyStatus.WAITING)
  private def notifyOfCreated(ref: ActorRef, sessionId: SessionId): Unit = {
    ref ! new LobbyResponse(LobbyStatus.CREATED, sessionId.id.toString)
    ref ! new LobbyResponse(LobbyStatus.CLOSE)
  }
}
