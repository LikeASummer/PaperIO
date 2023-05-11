package com.paper.io.ml

case class History(stored: Array[SensorFrame], size: Int) {
  def +:(record: SensorFrame): History = {
    stored match {
      case stored if stored.isEmpty => new History(Array.fill(size) {record}, size)
      case _ => new History((record +: stored).take(size), size)
    }
  }
  def unwrap(): Array[Array[Float]] = Array(stored.flatMap(_.unwrap()))
}

object History {
  def apply(size: Int): History = new History(Array(), size)
}
