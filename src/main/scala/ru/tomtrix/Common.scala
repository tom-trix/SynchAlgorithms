package ru.tomtrix

/** Base Message class to communicate among the actors */
sealed abstract class Message extends Serializable

/** Message exclusively for Starter (to inform the others to start modelling) */
object StartMessage extends Message
