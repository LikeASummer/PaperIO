package com.paper.io.domain

import java.util.UUID
case class ConnectionId(id: UUID)

object ConnectionId{
  def apply():ConnectionId = ConnectionId(UUID.randomUUID())
}
