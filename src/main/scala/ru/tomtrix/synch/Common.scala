package ru.tomtrix.synch

/** Base Message class to communicate among the actors
 * @param t timestamp
 * @param sender name of actor dending the message */
sealed abstract class Message(t: Double, sender: String) extends Serializable

/** Message exclusively for Starter (to inform the others to start modelling) */
object StartMessage extends Message(-1, "Starter")

/** Information message used basicly for debugging
 * @param sender name of actor dending the message
 * @param text message body */
case class InfoMessage(sender: String, text: String) extends Message(-1, sender)

/** Main message that brings the model event
 * @param t timestamp
 * @param sender name of actor dending the message
 * @param data message body */
case class EventMessage(t: Double, sender: String, data: Serializable) extends Message(t, sender)
