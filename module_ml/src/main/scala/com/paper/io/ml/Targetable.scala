package com.paper.io.ml

import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.{Field, Territory, Trajectory}
import com.paper.io.player.Player

trait TargetType {
  val code: Array[Float]
}

object TargetOwnTrajectory extends TargetType {
  override val code: Array[Float] = Array(1, 0, 0)
}

object TargetOwnTerritory extends TargetType {
  override val code: Array[Float] = Array(0, 1, 0)
}

object TargetGameField extends TargetType {
  override val code: Array[Float] = Array(0, 0, 1)
}

trait Targetable[A] {
  def target(entity: A, ray: Ray): Option[Position]
}

trait TargetableClass[A] {
  def entity: A
  def target(ray: Ray)(implicit observable: Targetable[A]): Option[Position] = observable.target(entity, ray)
}

object Targetable {

  implicit class TargetableTrajectory(override val entity: Trajectory) extends TargetableClass[Trajectory]
  implicit val visibleTrajectory: Targetable[Trajectory] = (entity: Trajectory, ray: Ray) => {
    intersectShape(entity.points.map(_.position).sliding(2), ray)
  }

  implicit class TargetableTerritory(override val entity: Territory) extends TargetableClass[Territory]
  implicit val visibleTerritory: Targetable[Territory] = (entity: Territory, ray: Ray) =>
    intersectShape(entity.points.sliding(2), ray)


  implicit class TargetableField(override val entity: Field) extends TargetableClass[Field]
  implicit val visibleField: Targetable[Field] = (entity: Field, ray: Ray) =>
    intersectShape(entity.points.sliding(2), ray).orElse(Some(ray.end))


  implicit class TargetablePlayer(override val entity: Player) extends TargetableClass[Player]
  implicit val visiblePlayer: Targetable[Player] = (entity: Player, ray: Ray) =>
    intersects(entity.previousPosition, entity.currentPosition, ray.start, ray.end)

  private def intersectShape(segments: Iterator[Vector[Position]], ray: Ray): Option[Position] = {
    val intersections = segments
      .flatMap(segment => intersects(segment.head, segment.last, ray.start, ray.end))

      if (intersections.isEmpty) {
        None
      } else {
        Some(intersections.min(Ordering.by[Position, Float](_.distance(ray.start))))
      }
  }

  private def intersects(p1: Position, p2: Position, q1: Position, q2: Position): Option[Position] = {
    val dp = p2 - p1
    val dq = q2 - q1

    val d = dq.x * dp.y - dq.y * dp.x

    if (d == 0){
      return None
    }

    val s = (dp.x * (q1.y - p1.y) + dp.y * (p1.x - q1.x)) / (dq.x * dp.y - dq.y * dp.x)
    val t = (dq.x * (p1.y - q1.y) + dq.y * (q1.x - p1.x)) / (dq.y * dp.x - dq.x * dp.y)

    if ((s >= 0) & (s <= 1) & (t >= 0) & (t <= 1))
      Some(Position(p1.x + t * dp.x, p1.y + t * dp.y))
    else
      None
  }

}