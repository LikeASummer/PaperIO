package com.paper.io.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import com.paper.io.geometry.complex.Radius

object Application {

  def main(args: Array[String]): Unit = {
    val config = new LwjglApplicationConfiguration()
    config.title = "PaperIO"
    config.width = 500
    config.height = 500
    config.foregroundFPS = 30
    new LwjglApplication(new GameController()(Radius(250F), 1F), config)
  }
}

class GameController(implicit val radius: Radius, val aspectRatio: Float) extends Game {
  override def create(): Unit = {
    setScreen(new MenuScreen(this))
  }
}