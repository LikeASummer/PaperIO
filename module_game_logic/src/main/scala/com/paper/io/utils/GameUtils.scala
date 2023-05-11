package com.paper.io.utils

import com.paper.io.geometry.base.{Direction, Position}

object GameUtils {
  def calculateDirection(rotation: Float, deltaTime: Float, direction: Direction): Direction = {
    val nextDirectionX: Float = (direction.x * Math.cos(rotation * deltaTime) - direction.y * Math.sin(rotation * deltaTime)).toFloat
    val nextDirectionY: Float = (direction.x * Math.sin(rotation * deltaTime) + direction.y * Math.cos(rotation * deltaTime)).toFloat
    Direction(nextDirectionX, nextDirectionY)
  }

  def calculatePosition(deltaTime: Float, direction: Direction, position: Position): Position = {
    position + (direction * 10 * deltaTime)
  }
}
