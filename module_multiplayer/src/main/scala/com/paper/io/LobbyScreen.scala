package com.paper.io

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Sink
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.{Game, Gdx, Screen}
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.paper.io.base.TextScreen
import com.paper.io.generated.{LobbyConnection, LobbyResponse, LobbyServiceClient, LobbyStatus}
import com.paper.io.geometry.complex.Radius

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class LobbyScreen(val controller: Game, val menuScreen: Game => Screen)(implicit val radius: Radius, val aspectRatio: Float) extends TextScreen {
  private implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "PlayClient")
  Gdx.input.setInputProcessor(stage)

  private val client = LobbyServiceClient(GrpcClientSettings.fromConfig("com.paper.io.LobbyService"))

  private val commandInputProcessor = Sink.foreach[LobbyResponse](processInputCommand)
  private val lobbyStatus = new AtomicReference[LobbyStatus](LobbyStatus.WAITING)
  private val sessionId = new AtomicReference("")
  private var timeout = 50F

  private def processInputCommand(data: LobbyResponse): Unit = {
    lobbyStatus.set(data.status)
    sessionId.set(data.sessionId)

    if (data.status == LobbyStatus.CREATED) {
      client.close()
    } else if (data.status == LobbyStatus.CLOSE) {
      client.close()
    }
  }

  override def show(): Unit = {
    client.connect(LobbyConnection(UUID.randomUUID().toString))
      .runWith(commandInputProcessor)

    layout.setFillParent(true)
    stage.addActor(layout)

    val labelStyle = new Label.LabelStyle()
    labelStyle.font = generateFont(30, Color.BLACK)

    val label = new Label("WAITING FOR OTHER PLAYERS", labelStyle)

    layout.add(label).fillX().uniformX()
  }

  override def onFrame(delta: Float): Unit = {
    timeout = timeout - delta

    if (lobbyStatus.get() == LobbyStatus.CREATED) {
      controller.setScreen(new MultiplayerScreen(controller, menuScreen, sessionId.get()))
    } else if (lobbyStatus.get() == LobbyStatus.CLOSE || timeout <= 0) {
      controller.setScreen(new ErrorScreen(controller, menuScreen))
    }
  }

  override def resize(width: Int, height: Int): Unit = {

  }

  override def pause(): Unit = {

  }

  override def resume(): Unit = {

  }

  override def hide(): Unit = {

  }

  override def dispose(): Unit = {
  }
}