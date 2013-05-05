package ru.tomtrix.synch

/**
 * HashSerializable
 */
trait HashSerializable extends Serializable {
  def toHash: String
}
