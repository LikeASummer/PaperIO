package com.paper.io

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.{Label, TextButton}
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.{Game, Gdx, Screen}
import com.paper.io.base.TextScreen

class ErrorScreen(controller: Game, menuScreen: Game => Screen) extends TextScreen {
  Gdx.input.setInputProcessor(stage)

  override def show(): Unit = {
    layout.setFillParent(true)
    stage.addActor(layout)

    val font = generateFont(30, Color.BLACK)

    val labelStyle = new Label.LabelStyle()
    labelStyle.font = font
    val label = new Label("CONNECTION LOST", labelStyle)

    val textButtonStyle = new TextButtonStyle()
    textButtonStyle.font = font
    textButtonStyle.fontColor = Color.BLACK

    val exit = new TextButton("Exit", textButtonStyle)

    layout.add(label).fillX().uniformX()
    layout.row().row()
    layout.row().row()
    layout.add(exit)

    exit.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = controller.setScreen(menuScreen(controller))
    })
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

  override def onFrame(delta: Float): Unit = {

  }
}