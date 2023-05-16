package com.paper.io.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.lwjgl3.{Lwjgl3Application, Lwjgl3ApplicationConfiguration}
import com.paper.io.geometry.complex.Radius

object Application {

  def main(args: Array[String]): Unit = {
    val config = new Lwjgl3ApplicationConfiguration()
    config.setWindowSizeLimits(500, 500, 500, 500)
    config.setTitle("PaperIO")
    config.setForegroundFPS(30)
    new Lwjgl3Application(new GameController()(Radius(250F), 1F), config)
  }
}

class GameController(implicit val radius: Radius, val aspectRatio: Float) extends Game {
  override def create(): Unit = {
    setScreen(new MenuScreen(this))
  }
}