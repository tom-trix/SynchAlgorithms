package ru.tomtrix

import util.Random
import compat.Platform

object Tester extends App with IModel {
  var state: Serializable = None

  def startModelling() {
    logger debug "Start OK"
    val rand = new Random(Platform.currentTime)
    for (i <- 0 to rand.nextInt(10)) {
      sendMessageToAll(Some("trix"))
      time += rand.nextInt(10)
    }
  }

  override def handleMessage(m: EventMessage) {
    super.handleMessage(m)
    logger info s"Yahoo! Принято сообщение от ${m.sender} с меткой ${m.t}"
  }
}
