package com.paper.io

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Sink
import com.paper.io.generated.{LobbyConnection, LobbyResponse, LobbyServiceClient, LobbyStatus}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
import org.scalatest._
import concurrent.Futures._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.flatspec.AsyncFlatSpec

class LobbyAsyncSpec extends AsyncFlatSpec with BeforeAndAfterAll with Matchers {

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

  "LobbyService" should "successfully connected to lobby and received session id" in {
    val client: LobbyServiceClient = LobbyServiceClient(GrpcClientSettings.fromConfig("com.paper.io.LobbyService"))
    val future = client.connect(LobbyConnection(UUID.randomUUID().toString)).runWith(Sink.seq)

    future.map(result => {
      client.close()
      assert(result.exists(result => result.status == LobbyStatus.CREATED))
    })
  }

  it should "eight clients successfully connected to lobby and received same session id" in {
    var futures = Vector[Future[Seq[LobbyResponse]]]()
    var clients = Vector[LobbyServiceClient]()
    for (_ <- 0 until 8) {
      val client = LobbyServiceClient(GrpcClientSettings.fromConfig("com.paper.io.LobbyService"))
      clients = client +: clients

      val future: Future[Seq[LobbyResponse]] = client.connect(LobbyConnection(UUID.randomUUID().toString)).runWith(Sink.seq)
      futures = future +: futures
    }

    Future.sequence(futures).map(result => {
      clients.foreach(_.close())
      val expectedSessionId = result.head.filter(_.status == LobbyStatus.CREATED).head.sessionId
      result.foreach(response => assert(response.exists(_.sessionId == expectedSessionId)))
      succeed
    })
  }
}
