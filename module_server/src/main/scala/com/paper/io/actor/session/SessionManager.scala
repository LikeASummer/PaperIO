package com.paper.io.actor.session

import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import com.paper.io.actor.session.Session.{Connect, Disconnect, SessionMessage}
import com.paper.io.domain.{ConnectionId, SessionId}
import com.paper.io.generated.PlayerInput

object SessionManager {

  trait SessionManagerMessage
  final case class ConnectToSession(sessionId: SessionId, connectionId: ConnectionId, outputSink: akka.actor.ActorRef, inputSource: Source[PlayerInput, NotUsed]) extends SessionManagerMessage
  final case class DisconnectFromSession(sessionId: SessionId, connectionId: ConnectionId) extends SessionManagerMessage

  def apply(sessions: Map[SessionId, ActorRef[SessionMessage]] = Map.empty): Behavior[SessionManagerMessage] = Behaviors.setup { _ =>
      Behaviors.receive[SessionManagerMessage] { case (context, message) =>
        message match {
          case ConnectToSession(sessionId, connectionId, outputSink, inputSource) =>
            var newSessions = sessions

            if (!sessions.contains(sessionId)) {
              context.log.info(s"Creating new session: $sessionId")
              val actorRef = context.spawn(Session.create(), s"session-${sessionId.id.toString}")
              context.watch(actorRef)
              newSessions = sessions + (sessionId -> actorRef)
            }

            for (actorRef <- newSessions.get(sessionId)) {
              context.log.info(s"Passing connection data to session: $sessionId")
              actorRef ! Connect(connectionId, outputSink, inputSource)
            }

            apply(newSessions)
          case DisconnectFromSession(sessionId, connectionId) =>
            if (sessions.contains(sessionId)) {
              context.log.info(s"Passing disconnection data to session: $sessionId")
              for (actorRef <- sessions.get(sessionId)) {
                actorRef ! Disconnect(connectionId)
              }
            }

            apply(sessions)
        }
      }.receiveSignal {
          case (context, Terminated(ref: ActorRef[_])) =>
            context.log.info("Terminating session")
            apply(sessions.filter{ case (_, actorRef) => !actorRef.eq(ref) })
        }
    }
}
