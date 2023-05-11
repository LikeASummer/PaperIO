package com.paper.io

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.scaladsl.{Metadata, ServiceHandler}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.pki.pem.{DERPrivateKeyLoader, PEMDecoder}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.paper.io.actor.lobby.Lobby.{ConnectToLobby, DisconnectFromLobby}
import com.paper.io.actor.lobby.Lobby
import com.paper.io.actor.session.SessionManager
import com.paper.io.actor.session.SessionManager.{ConnectToSession, DisconnectFromSession}
import com.paper.io.domain.{ConnectionId, SessionId}
import com.paper.io.generated.{LobbyConnection, LobbyResponse, LobbyServicePowerApi, LobbyServicePowerApiHandler, LobbyStatus, PlayerInput, SessionMetadata, SessionServicePowerApi, SessionServicePowerApiHandler, SessionState}
import com.typesafe.config.ConfigFactory

import java.security.{KeyStore, SecureRandom}
import java.security.cert.{Certificate, CertificateFactory}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Server {
  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())

    val actorSystem = ActorSystem[Nothing](Behaviors.empty, "PaperIOServer", conf)
    new Server(actorSystem).run()
  }
}

class Server(actorSystem: ActorSystem[_]) {

  def run(): Future[Http.ServerBinding] = {
    implicit val system: ActorSystem[_] = actorSystem
    implicit val executionContext: ExecutionContext = system.executionContext

    val lobbyService = LobbyServicePowerApiHandler.partial(new LobbyServiceImpl())
    val sessionService = SessionServicePowerApiHandler.partial(new SessionServiceImpl())
    val serviceHandlers = ServiceHandler.concatOrNotFound(lobbyService, sessionService)

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface = "127.0.0.1", port = 8080)
      .enableHttps(serverHttpContext)
      .bind(serviceHandlers)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println("gRPC server bound to {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        println("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }

    bound
  }

  private def serverHttpContext: HttpsConnectionContext = {
    val privateKey = DERPrivateKeyLoader.load(PEMDecoder.decode(readPrivateKeyPem()))
    val fact = CertificateFactory.getInstance("X.509")
    val cer = fact.generateCertificate(classOf[Server].getResourceAsStream("/server.pem"))

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry("private", privateKey, new Array[Char](0), Array[Certificate](cer))

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    ConnectionContext.httpsServer(context)
  }

  private def readPrivateKeyPem(): String = scala.io.Source.fromResource("server.key").mkString

  private class LobbyServiceImpl(implicit system: ActorSystem[_], executionContext: ExecutionContext) extends LobbyServicePowerApi {
    private val lobby = system.systemActorOf(Lobby.create()(8, 20.seconds), "lobby")

    override def connect(in: LobbyConnection, metadata: Metadata): Source[LobbyResponse, NotUsed] = {
      val (ref, sourcePublisher) = Source.actorRef[LobbyResponse](
        completionMatcher = PartialFunction.empty,
        failureMatcher = PartialFunction.empty,
        bufferSize = 2,
        overflowStrategy = OverflowStrategy.dropHead)
        .toMat(Sink.asPublisher(true))(Keep.both).run()

      val connectionId = ConnectionId()
      lobby ! ConnectToLobby(connectionId, ref)

      val source = Source.fromPublisher[LobbyResponse](sourcePublisher)
        .takeWhile(response => response.status != LobbyStatus.CLOSE)

      source.watchTermination() {
        (_, future) =>
          future.onComplete(_ => lobby ! DisconnectFromLobby(connectionId))
          NotUsed.notUsed()
      }
    }
  }
}

private class SessionServiceImpl(implicit system: ActorSystem[_], executionContext: ExecutionContext) extends SessionServicePowerApi {
  private val sessionManager = system.systemActorOf(SessionManager(), "session_manager")

   override def play(in: Source[PlayerInput, NotUsed], metadata: Metadata): Source[SessionMetadata, NotUsed] = {
     val (ref, sourcePublisher) = Source.actorRef[SessionMetadata](
       completionMatcher = PartialFunction.empty,
       failureMatcher = PartialFunction.empty,
       bufferSize = 2,
       overflowStrategy = OverflowStrategy.dropHead)
       .toMat(Sink.asPublisher(true))(Keep.both).run()

     val id = ConnectionId()
     val sessionId = SessionId(metadata.getText("sessionId").get)

     sessionManager ! ConnectToSession(sessionId, id, ref, in)

     val source = Source.fromPublisher[SessionMetadata](sourcePublisher)
       .idleTimeout(15.seconds)
       .takeWhile(response => {
         response.state != SessionState.CLOSE_CONNECTION
       })

     source.watchTermination() {
       (_, future) =>
         future.onComplete(_ => sessionManager ! DisconnectFromSession(sessionId, id))
         NotUsed.notUsed()
     }
   }
}