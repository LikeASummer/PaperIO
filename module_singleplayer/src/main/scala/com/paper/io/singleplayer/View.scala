package com.paper.io.singleplayer

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.paper.io.geometry.base.Position
import com.paper.io.geometry.complex.{Field, Territory, Trajectory}
import com.paper.io.player.{Color, Player}

trait View[A] {
  def paint(entity: A, color: Color)(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit
}

trait ViewClass[A] {
  def entity: A
  def paint(color: Color)(implicit view: View[A], shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = view.paint(entity, color)
}

case class ViewCoordinateCorrection(x: Float, y: Float)

object View {

  implicit class ViewTrajectory(override val entity: Trajectory) extends ViewClass[Trajectory]

  implicit val trajectoryView: View[Trajectory] = new View[Trajectory] {
    override def paint(entity: Trajectory, color: Color)(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = {
      shapeRenderer.setColor(color.red, color.green, color.blue, 0.5F)
      shapeRenderer.begin(ShapeType.Filled)
      paintLine(entity.points.map(_.position))
      shapeRenderer.end()
    }
  }

  implicit class ViewTerritory(override val entity: Territory) extends ViewClass[Territory]

  implicit val territoryView: View[Territory] = new View[Territory] {
    override def paint(entity: Territory, color: Color)(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = {
      shapeRenderer.setColor(color.red, color.green, color.blue, 0.5F)
      shapeRenderer.begin(ShapeType.Filled)
      paintLine(entity.points :+ entity.points.head)
      shapeRenderer.end()
    }
  }

  implicit class ViewField(override val entity: Field) extends ViewClass[Field]

  implicit val fieldView: View[Field] = new View[Field] {
    override def paint(entity: Field, color: Color)(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = {
      shapeRenderer.setColor(color.red, color.green, color.blue, 0.5F)
      shapeRenderer.begin(ShapeType.Filled)
      paintLine(entity.points)
      shapeRenderer.end()
    }
  }

  implicit class ViewPlayer(override val entity: Player) extends ViewClass[Player]

  implicit val playerView: View[Player] = new View[Player] {
    override def paint(entity: Player, color: Color)(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = {
      shapeRenderer.setColor(color.red, color.green, color.blue, 1)

      entity.territory.paint(color)
      entity.trajectory.paint(color)

      val position = Position(entity.previousPosition.x + viewCoordinateCorrection.x, entity.previousPosition.y + viewCoordinateCorrection.y)
      shapeRenderer.begin(ShapeType.Filled)
      shapeRenderer.circle(position.x, position.y, 2, 100)
      shapeRenderer.end()
    }
  }

  private def paintLine(points: Vector[Position])(implicit shapeRenderer: ShapeRenderer, viewCoordinateCorrection: ViewCoordinateCorrection): Unit = {
    points.map(point => Position(point.x + viewCoordinateCorrection.x, point.y + viewCoordinateCorrection.y))
      .sliding(2).foreach(window => {
      shapeRenderer.line(window.head.x, window.head.y, window.last.x, window.last.y)
    })
  }
}
