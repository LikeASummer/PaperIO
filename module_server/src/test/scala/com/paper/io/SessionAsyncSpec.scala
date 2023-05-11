package com.paper.io

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.internal.{NettyClientUtils, ScalaBidirectionalStreamingRequestBuilder}
import akka.grpc.scaladsl.StreamResponseRequestBuilder
import akka.grpc.{GrpcChannel, GrpcClientCloseException, GrpcClientSettings}
import akka.http.scaladsl.Http
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.paper.io.generated.{PlayerInput, SessionService, SessionServiceClient}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, _}
import org.scalatest.concurrent.Futures._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.must.Matchers

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}


object EnhancedSessionServiceClient {
  def apply(settings: GrpcClientSettings, sessionId: String)(implicit sys: ClassicActorSystemProvider): SessionServiceClient =
    new DefaultSessionServiceClient(GrpcChannel(settings), isChannelOwned = true, sessionId)

  def apply(channel: GrpcChannel, sessionId: String)(implicit sys: ClassicActorSystemProvider): SessionServiceClient =
    new DefaultSessionServiceClient(channel, isChannelOwned = false, sessionId)

  private class DefaultSessionServiceClient(channel: GrpcChannel, isChannelOwned: Boolean, sessionId: String)(implicit sys: ClassicActorSystemProvider) extends SessionServiceClient {

    import SessionService.MethodDescriptors._

    private implicit val ex: ExecutionContext = sys.classicSystem.dispatcher
    private val settings = channel.settings
    private val options = NettyClientUtils.callOptions(settings)

    private def playRequestBuilder(channel: akka.grpc.internal.InternalChannel) =
      new ScalaBidirectionalStreamingRequestBuilder(playDescriptor, channel, options, settings)
        .addHeader("sessionId", sessionId)

    override def play(): StreamResponseRequestBuilder[akka.stream.scaladsl.Source[com.paper.io.generated.PlayerInput, akka.NotUsed], com.paper.io.generated.SessionMetadata] =
      playRequestBuilder(channel.internalChannel)

    def play(in: akka.stream.scaladsl.Source[com.paper.io.generated.PlayerInput, akka.NotUsed]): akka.stream.scaladsl.Source[com.paper.io.generated.SessionMetadata, akka.NotUsed] =
      play().invoke(in)


    override def close(): scala.concurrent.Future[akka.Done] =
      if (isChannelOwned) channel.close()
      else throw new GrpcClientCloseException()

    override def closed: scala.concurrent.Future[akka.Done] = channel.closed()
  }
}

class SessionAsyncSpec extends AsyncFlatSpec with BeforeAndAfterAll with Matchers {

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  val conf: Config = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on").withFallback(ConfigFactory.defaultApplication())
  val testKit: ActorTestKit = ActorTestKit(conf)
  val serverSystem: ActorSystem[_] = testKit.system
  val bound: Future[Http.ServerBinding] = new Server(serverSystem).run()

  bound.futureValue

  implicit val clientSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "PlayService")
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  override def afterAll: Unit = {
    ActorTestKit.shutdown(clientSystem)
    testKit.shutdownTestKit()
  }

  def createSource(): Source[PlayerInput, NotUsed] = {
    val (_, sourcePublisher) = Source.actorRef[PlayerInput](
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = 2,
      overflowStrategy = OverflowStrategy.fail)
      .toMat(Sink.asPublisher(true))(Keep.both).run()

    Source.fromPublisher[PlayerInput](sourcePublisher)
  }

  "SessionServiceClient" should "successfully connected to session and start receiving game data" in {
    val client: SessionServiceClient = EnhancedSessionServiceClient(GrpcClientSettings.fromConfig("com.paper.io.SessionService"), UUID.randomUUID().toString)
    val future = client.play(createSource()).takeWithin(50.seconds).runWith(Sink.seq)

    future.map(result => {
      client.close()
      assert(result.exists(result => result.state.isInProgress))
    })
  }
}
