package ru.tomtrix

object Tester extends App {
  var communicator: Communicator = _

  communicator = new Communicator(new ModelLoadable {
    def startModelling() {
      communicator.actors foreach {_ ! communicator.actorname}
      communicator.actors foreach {_ ! communicator.actorname}
    }
  })
}
