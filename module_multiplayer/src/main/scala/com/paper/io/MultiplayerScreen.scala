package com.paper.io

import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.internal.{NettyClientUtils, ScalaBidirectionalStreamingRequestBuilder}
import akka.grpc.scaladsl.StreamResponseRequestBuilder
import akka.grpc.{GrpcChannel, GrpcClientCloseException, GrpcClientSettings}
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.{BitmapFont, SpriteBatch}
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.{Game, Gdx, InputAdapter, Screen}
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.paper.io.generated.{PlayerColor, PlayerInput, SessionMetadata, SessionService, SessionServiceClient, SessionState, Vector2D}
import com.paper.io.geometry.base.{Direction, Position}
import com.paper.io.geometry.complex.Radius
import com.paper.io.utils.GameUtils.{calculateDirection, calculatePosition}
import akka.NotUsed
import akka.actor.{ActorRef, ClassicActorSystemProvider}
import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.paper.io.utils.ModelTransformer.calculateAreaOfPoints
import com.paper.io.utils.ViewUtils.ViewCoordinateCorrection

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

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

class MultiplayerScreen(val controller: Game, val menuScreen: Game => Screen, val sessionId: String)(implicit val radius: Radius, val aspectRatio: Float) extends Screen {
  private implicit val shapeRenderer: ShapeRenderer = new ShapeRenderer()
  private implicit val viewCoordinateCorrection: ViewCoordinateCorrection = ViewCoordinateCorrection(radius.value, radius.value)
  private implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "PlayClient")

  private val cam: OrthographicCamera = new OrthographicCamera(100, 100 * aspectRatio)
  private val spriteBatch = new SpriteBatch()

  private val generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/times_bold.ttf"))
  private val parameter = new FreeTypeFontGenerator.FreeTypeFontParameter()
  private var font: Option[BitmapFont] = None

  private val client = EnhancedSessionServiceClient(GrpcClientSettings.fromConfig("com.paper.io.SessionService"), sessionId)

  private val (commandOutputActor, commandOutputSource) = createSource()
  private val commandInputProcessor = Sink.foreach[SessionMetadata](processInputCommand)

  private val playerId: AtomicReference[String] = new AtomicReference[String]("")
  private val color: AtomicReference[PlayerColor] = new AtomicReference[PlayerColor](PlayerColor())
  private val metadata: AtomicReference[SessionMetadata] = new AtomicReference[SessionMetadata]()
  private val gameState = new AtomicReference[SessionState](SessionState.WAITING_FOR_PLAYERS)

  private var position = Position()
  private var direction = Direction(1F, 0)

  private val angle = new AtomicReference(0F)

  private var gameWasWon = false
  private var exitTimeout = 3f

  private def createSource(): (ActorRef, Source[PlayerInput, NotUsed]) = {
    val (ref, sourcePublisher) = Source.actorRef[PlayerInput](
      completionMatcher = PartialFunction.empty,
      failureMatcher = PartialFunction.empty,
      bufferSize = 2,
      overflowStrategy = OverflowStrategy.fail)
      .toMat(Sink.asPublisher(true))(Keep.both).run()

    val source = Source.fromPublisher[PlayerInput](sourcePublisher)
      .idleTimeout(15.seconds)

    (ref, source)
  }

  private def processInputCommand(data: SessionMetadata): Unit = {
    gameState.set(data.state)
    metadata.set(data)

    if (data.state == SessionState.IN_PROGRESS && data.players.nonEmpty && playerId.get().isEmpty) {
      val initial = data.players
        .filter(d => d.playerId == data.playerId)
        .head

      for (receivedColor <- initial.color) {
        color.set(receivedColor)
      }

      playerId.set(data.playerId)
      for (positionVector <- initial.position) {
        position = Position(positionVector.x, positionVector.y)
      }
    } else if (data.state == SessionState.ENDED) {
      val numberOfSurvivedPlayers = metadata.get().players.count(gameState => gameState.isActive)
      val isPlayerDisabled = metadata.get().players.exists(gameState => gameState.playerId == playerId.get() && !gameState.isActive)

      if (isPlayerDisabled || numberOfSurvivedPlayers == 1) {
        if (isPlayerDisabled) {
          gameWasWon = false
        } else {
          gameWasWon = true
        }
      }
      client.close()
    }
  }

  override def show(): Unit = {
    parameter.size = 30
    font = Some(generator.generateFont(parameter))
    generator.dispose()

    cam.position.set(0 + viewCoordinateCorrection.x, 0 + viewCoordinateCorrection.y, 0)
    cam.update()

    client.play(commandOutputSource).runWith(commandInputProcessor)

    Gdx.input.setInputProcessor(new InputAdapter() {
      override def keyDown(keycode: Int): Boolean = {
        keycode match {
          case Keys.D => angle.set(-2F)
          case Keys.A => angle.set(2F)
          case _ =>
        }

        true
      }

      override def keyUp(keycode: Int): Boolean = {
        keycode match {
          case Keys.D => angle.set(0F)
          case Keys.A => angle.set(0F)
          case _ =>
        }

        true
      }
    })
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(1, 1, 1, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    gameState.get() match {
      case SessionState.WAITING_FOR_PLAYERS => waiting()
      case SessionState.IN_PROGRESS => playGame(delta)
      case SessionState.ENDED => showResult(delta)
      case SessionState.CLOSE_CONNECTION => showResult(delta)
    }
  }

  private def waiting(): Unit = {
    spriteBatch.begin()
    font.get.setColor(0, 0, 0, 1)
    font.get.draw(spriteBatch, "CONNECTING", 150, 250)
    spriteBatch.end()
  }

  private def showResult(delta: Float): Unit = {
    spriteBatch.begin()
    font.get.setColor(0, 0, 0, 1)
    if (gameWasWon) {
      font.get.draw(spriteBatch, "YOU WON THE GAME", 110, 250)
    } else {
      font.get.draw(spriteBatch, "YOU LOST THE GAME", 110, 250)
    }
    spriteBatch.end()

    exitTimeout = exitTimeout - delta
    if (exitTimeout <= 0) {
      controller.setScreen(menuScreen(controller))
    }
  }

  private def playGame(delta: Float): Unit = {
    val id = playerId.get()
    direction = calculateDirection(angle.get(), delta, direction)
    position = calculatePosition(delta, direction, position)

    shapeRenderer.setColor(color.get.red, color.get.green, color.get.blue, 1)
    shapeRenderer.begin(ShapeType.Filled)
    shapeRenderer.circle(position.x + viewCoordinateCorrection.x, position.y + viewCoordinateCorrection.y, 2, 100)
    shapeRenderer.end()

    commandOutputActor ! PlayerInput(id, Some(Vector2D(position.x, position.y)), Some(Vector2D(direction.x, direction.y)))

    cam.position.set(position.x + viewCoordinateCorrection.x, position.y + viewCoordinateCorrection.y, 0)
    cam.update()

    shapeRenderer.setProjectionMatrix(cam.combined)

    val data = metadata.get()
    data.players.filter(_.isActive).foreach(other => {
      shapeRenderer.setColor(other.color.get.red, other.color.get.green, other.color.get.blue, 1)

      if (!id.contains(other.playerId)) {
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.circle(other.position.get.x + viewCoordinateCorrection.x, other.position.get.y + viewCoordinateCorrection.y, 2, 100)
        shapeRenderer.end()
      }

      shapeRenderer.setColor(other.color.get.red, other.color.get.green, other.color.get.blue, 0.3F)
      paintLine(other.territory)
      paintLine(other.trajectory)
    })

    shapeRenderer.setColor(0, 0, 0, 1)
    paintLine(data.field)

    val orderedArea = metadata.get().players
      .filter(_.isActive)
      .map(player => (player.color.getOrElse(PlayerColor()), 100 * (player.territory / metadata.get().field)))
      .toVector
      .sortBy(_._2)
      .reverse

    var counter = 1
    var height = 15

    for ((color, percentage) <- orderedArea) {
      spriteBatch.begin()
      font.get.setColor(color.red, color.green, color.blue, 1)
      font.get.draw(spriteBatch, f"$counter : $percentage%2.2f" + " %", 350, 500 - height)
      spriteBatch.end()

      height += 25
      counter += 1
    }
  }

  private def paintLine(points: Iterable[Vector2D])(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = {
    shapeRenderer.begin(ShapeType.Filled)
    points.map(point => Vector2D(point.x + viewCoordinateCorrection.x, point.y + viewCoordinateCorrection.y))
      .sliding(2).foreach(window => {
      shapeRenderer.rectLine(window.head.x, window.head.y, window.last.x, window.last.y, 2)
    })
    shapeRenderer.end()
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
    shapeRenderer.dispose()
  }
}
