package ru.tomtrix

import org.apache.log4j.Logger

/** Trait to add the logging functionality. Modify "log4j.properties" file to get your own settings */
trait Loggable {
  lazy val logger = Logger getLogger this.getClass

  def $(s: Any) {
    logger debug s
  }
}
