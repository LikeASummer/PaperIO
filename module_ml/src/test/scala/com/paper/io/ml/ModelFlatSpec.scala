package com.paper.io.ml

import com.paper.io.geometry.base.Position
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.{Failure, Success}

class ModelFlatSpec extends AnyFlatSpec {
  "Model" should "be created from model configuration" in {
    assert(Model("correct.json").isSuccess)
  }

  it should "produce error when created from un-existent file" in {
    assert(Model("not_exist.json").isFailure)
  }

  it should "produce error when created from faulty file" in {
    assert(Model("faulty.json").isFailure)
  }

  it should "produce decision when data is sent to it" in {
    Model("correct.json") match {
      case Failure(_) => fail("Model supposed to be created")
      case Success(model) =>
        val sensorPlayer = SensorPlayer(0, 0, 0, 0, 0)
        val sensorTargets = Vector.fill(7)(SensorTarget(0, TargetGameField, Position()))
        val frame = SensorFrame(sensorPlayer, sensorTargets)

        val nextModel = frame +: model
        assert(nextModel.decision() != Unknown)
    }
  }
}
