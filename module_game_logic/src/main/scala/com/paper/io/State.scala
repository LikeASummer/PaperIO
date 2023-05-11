package com.paper.io

sealed trait State

case object WaitingForPlayers extends State
case object InProgress extends State
case object Ended extends State
