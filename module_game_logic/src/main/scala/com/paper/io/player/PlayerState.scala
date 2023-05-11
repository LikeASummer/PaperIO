package com.paper.io.player

sealed trait PlayerState {}

case object Active extends PlayerState
case object Disabled extends PlayerState
