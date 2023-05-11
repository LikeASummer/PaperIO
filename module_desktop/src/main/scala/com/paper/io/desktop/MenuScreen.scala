package com.paper.io.desktop

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.{Table, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.{Actor, Stage}
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.{Game, Gdx, Screen}
import com.paper.io.{LobbyScreen, MultiplayerScreen}
import com.paper.io.geometry.complex.Radius
import com.paper.io.singleplayer.SingleplayerScreen

import java.util.UUID

class MenuScreen(val controller: Game)(implicit val radius: Radius, val aspectRatio: Float) extends Screen {
  private val stage = new Stage(new ScreenViewport)
  Gdx.input.setInputProcessor(stage)

  private val layout = new Table()

  override def show(): Unit = {
    layout.setFillParent(true)
    stage.addActor(layout)

    val generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/times_bold.ttf"))
    val parameter = new FreeTypeFontGenerator.FreeTypeFontParameter()

    parameter.size = 40
    val font = generator.generateFont(parameter)
    generator.dispose()

    val textButtonStyle = new TextButtonStyle()
    textButtonStyle.font = font
    textButtonStyle.fontColor = Color.BLACK

    val singleplayer = new TextButton("Singleplayer", textButtonStyle)
    val multiplayer = new TextButton("Multiplayer", textButtonStyle)
    val exit = new TextButton("Exit", textButtonStyle)

    layout.add(singleplayer).fillX().uniformX()
    layout.row().row()
    layout.add(multiplayer).fillX().uniformX()
    layout.row().row()
    layout.add(exit).fillX().uniformX()

    exit.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = Gdx.app.exit()
    })

    singleplayer.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
        controller.setScreen(new SingleplayerScreen(controller, controller => new MenuScreen(controller)))
      }
    })

    multiplayer.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
        controller.setScreen(new LobbyScreen(controller, controller => new MenuScreen(controller)))
      }
    })
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    stage.draw()
    stage.act()
  }

  override def resize(width: Int, height: Int): Unit = {}

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def hide(): Unit = {}

  override def dispose(): Unit = {}
}
