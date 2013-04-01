package ru.tomtrix

/** Abstract trait that your model should implement */
trait ModelLoadable extends Serializable {
  def startModelling()
}
