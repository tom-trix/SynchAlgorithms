package ru.tomtrix.synch

import org.apache.log4j.Logger

/**
 * Loggable trait
 */
trait Loggable {
  /** instance of a logger */
  lazy val logger = Logger getLogger this.getClass

  /** Syntax sugar for "logger.debug(s)"<br>Since Scala 2.10 it's recommended to use [[ru.tomtrix.synch.ApacheLogger.LogHelper#log log method]] instead
    * @param s smth to log
    */
  def $(s: => Any) {
    logger debug s
  }
}
