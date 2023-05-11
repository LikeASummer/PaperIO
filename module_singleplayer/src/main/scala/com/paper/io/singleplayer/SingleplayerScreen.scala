package com.paper.io.singleplayer

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.{Game, Gdx, InputAdapter, Screen}
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera}
import com.badlogic.gdx.graphics.g2d.{BitmapFont, SpriteBatch}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.paper.io
import com.paper.io.geometry.complex.Radius
import com.paper.io.ml.{Model, ModelDecision, Sensor, TurnLeft, TurnRight}
import com.paper.io.player.{Color, PlayerId}
import com.paper.io.singleplayer.View.{ViewField, ViewPlayer}
import com.paper.io.utils.GameUtils.calculateDirection

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

class SingleplayerScreen(val controller: Game, val mainScreen: Game => Screen)(implicit val radius: Radius, val aspectRatio: Float) extends Screen {
  private implicit val shapeRenderer: ShapeRenderer = new ShapeRenderer()
  private implicit val viewCoordinateCorrection: ViewCoordinateCorrection = ViewCoordinateCorrection(radius.value, radius.value)

  private val cam: OrthographicCamera = new OrthographicCamera(100, 100 * aspectRatio)
  private val spriteBatch = new SpriteBatch()

  private val game = new AtomicReference(io.Game(fieldRadius = 100F, startTerritory = 20, numberOfPlayers = 8))
  private val artificialPlayers: Map[PlayerId, Model] = (0 until 7)
    .map(_ => (PlayerId(), Model("models/model.json").get))
    .toMap

  private var artificialPlayersAngle = Map[PlayerId, Float]()
  private val artificialPlayerDecisionEveryNFrame = 15

  private val playerAngle = new AtomicInteger(0)
  private val playerId = PlayerId()

  private var frameCounter = artificialPlayerDecisionEveryNFrame

  private val generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/times_bold.ttf"))
  private val parameter = new FreeTypeFontGenerator.FreeTypeFontParameter()
  private var font: Option[BitmapFont] = None

  private var gameEnded = false
  private var gameWasWon = false
  private var exitTimeout = 3f

  override def show(): Unit = {

    parameter.size = 20
    font = Some(generator.generateFont(parameter))
    generator.dispose()

    cam.position.set(0 + viewCoordinateCorrection.x, 0 + viewCoordinateCorrection.y, 0)
    cam.update()

    game.getAndUpdate(_.addPlayer(playerId))
    for (artificialPlayersId <- artificialPlayers.keys) {
      game.getAndUpdate(_.addPlayer(artificialPlayersId))
    }

    game.getAndUpdate(_.startGame())

    Gdx.input.setInputProcessor(new InputAdapter() {
      override def keyDown(keycode: Int): Boolean = {
        keycode match {
          case Keys.D => playerAngle.getAndUpdate(_ => -2)
          case Keys.A => playerAngle.getAndUpdate(_ => 2)
          case _ =>
        }

        true
      }

      override def keyUp(keycode: Int): Boolean = {
        keycode match {
          case Keys.D => playerAngle.getAndUpdate(_ => 0)
          case Keys.A => playerAngle.getAndUpdate(_ => 0)
          case _ =>
        }

        true
      }
    })
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(1, 1, 1, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    if (gameEnded) {
      showResult(delta)
    } else {
      playGame(delta)
    }
  }

  private def showResult(delta: Float): Unit = {
    spriteBatch.begin()
    font.get.setColor(0, 0, 0, 1)
    if (gameWasWon) {
      font.get.draw(spriteBatch, "YOU WON THE GAME", 150, 250)
    } else {
      font.get.draw(spriteBatch, "YOU LOST THE GAME", 150, 250)
    }
    spriteBatch.end()

    exitTimeout  = exitTimeout - delta
    if (exitTimeout <= 0) {
      controller.setScreen(mainScreen(controller))
    }
  }

  private def playGame(delta: Float): Unit = {
    Gdx.gl.glClearColor(1, 1, 1, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)

    frameCounter -= 1

    val player = game.get().players(playerId)
    val nextDirection = calculateDirection(playerAngle.get(), delta, player.direction)
    val nextPosition = player.currentPosition + (nextDirection * 10 * delta)
    game.getAndUpdate(_.update(playerId, nextPosition, nextDirection))

    cam.position.set(nextPosition.x + viewCoordinateCorrection.x, nextPosition.y + viewCoordinateCorrection.y, 0)
    cam.update()

    shapeRenderer.setProjectionMatrix(cam.combined)

    for (artificialPlayerId <- artificialPlayers.keys) {
      val artificialPlayer = game.get().players(artificialPlayerId)

      if (artificialPlayer.isActive) {
        var model = artificialPlayers(artificialPlayerId)
        val frame = Sensor.sense(artificialPlayer, game.get().field, model.angles)
        model = frame +: model

        if (frameCounter == 0) {
          val decision: ModelDecision = model.decision()

          val angle = decision match {
            case TurnLeft => 2F
            case TurnRight => -2F
            case _ => 0F
          }

          artificialPlayersAngle = artificialPlayersAngle + (artificialPlayerId -> angle)
        }

        val nextDirection = calculateDirection(artificialPlayersAngle.getOrElse(artificialPlayerId, 0), delta, artificialPlayer.direction)
        val nextPosition = artificialPlayer.currentPosition + (nextDirection * 10 * delta)

        game.getAndUpdate(_.update(artificialPlayerId, nextPosition, nextDirection))
      }
    }

    if (frameCounter == 0) {
      frameCounter = artificialPlayerDecisionEveryNFrame
    }

    game.get().field.paint(Color(0, 0, 0))
    for (player <- game.get().players.values) {
      if (player.isActive) {
        player.paint(player.color)
      }
    }

    val field = game.get().field
    val orderedArea = game.get().players.values
      .filter(_.isActive)
      .map(player => (player.color, 100 * (player.territory.area() / field.area())))
      .toVector
      .sortBy(_._2)
      .reverse

    var counter = 1
    var height = 15

    for ((color, percentage) <- orderedArea) {
      spriteBatch.begin()
      font.get.setColor(color.red, color.green, color.blue, 1)
      font.get.draw(spriteBatch, f"$counter : $percentage%2.2f" + " %", 400, 500 - height)
      spriteBatch.end()

      height += 20
      counter += 1
    }

    val numberOfSurvivedPlayers = game.get().players.count{ case (_, player) => player.isActive}
    val isPlayerDisabled = !game.get().players(playerId).isActive

    if (isPlayerDisabled || numberOfSurvivedPlayers == 1) {
      game.getAndUpdate(_.endGame())
      gameEnded = true

      if (isPlayerDisabled) {
        gameWasWon = false
      } else {
        gameWasWon = true
      }
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
    shapeRenderer.dispose()
  }
}
