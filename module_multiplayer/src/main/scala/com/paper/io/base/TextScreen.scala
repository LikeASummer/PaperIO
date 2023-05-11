package com.paper.io.base

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.{Gdx, Screen}
import com.badlogic.gdx.graphics.{Color, GL20}
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport

abstract class TextScreen extends Screen {
  val stage = new Stage(new ScreenViewport)
  val layout = new Table()

  def generateFont(size: Int, color: Color): BitmapFont = {
    val generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/times_bold.ttf"))
    val parameter = new FreeTypeFontGenerator.FreeTypeFontParameter()

    parameter.size = size
    parameter.color = color

    val font: BitmapFont = generator.generateFont(parameter)
    generator.dispose()

    font
  }

  override def render(delta: Float): Unit = {
    Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    stage.draw()
    stage.act()
    onFrame(delta)
  }

  def onFrame(delta: Float): Unit

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
