package ru.tomtrix

/** Base Message class to communicate among the actors */
sealed abstract class Message(t: Double, sender: String) extends Serializable

/** Message exclusively for Starter (to inform the others to start modelling) */
object StartMessage extends Message(-1, "Starter")

case class InfoMessage(sender: String, text: String) extends Message(-1, sender)

case class EventMessage(t: Double, sender: String, data: Serializable) extends Message(t, sender)
