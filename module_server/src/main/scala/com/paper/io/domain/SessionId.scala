package com.paper.io.domain

import java.util.UUID
case class SessionId(id: UUID)

object SessionId{
  def apply():SessionId = SessionId(UUID.randomUUID())
  def apply(value: String):SessionId = SessionId(UUID.fromString(value))
}
