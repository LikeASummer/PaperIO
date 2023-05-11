package com.paper.io.geometry.special

import com.paper.io.geometry.base.Position

import scala.language.implicitConversions

case class OwnedPosition(position: Position, owner: Owner)

object OwnedPosition {
  def apply(position: Position): OwnedPosition = new OwnedPosition(position, EmptyOwner)
  def unapply(ownedPosition: OwnedPosition): Option[(Position, Owner)] = Option((ownedPosition.position, ownedPosition.owner))

  implicit def positionToOwnedPosition(position: Position): OwnedPosition = new OwnedPosition(position, EmptyOwner)
}