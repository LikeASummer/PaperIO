package com.paper.io.player

import com.paper.io.geometry.special.Owner

import java.util.UUID

case class PlayerId(value: UUID) extends Owner {
  override def toString: String = value.toString
}

object PlayerId{
  def apply(): PlayerId = new PlayerId(UUID.randomUUID())
  def apply(value: UUID): PlayerId = new PlayerId(value)
  def apply(value: String): PlayerId = new PlayerId(UUID.fromString(value))
}
