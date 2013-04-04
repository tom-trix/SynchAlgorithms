package ru.tomtrix

import org.apache.log4j.Logger

object Logg {
  lazy val logger = Logger getLogger this.getClass

  /** String interpolation feature */
  implicit class LogHelper(val sc: StringContext) extends AnyVal {
    def log(args: Any*) {
      val strings = sc.parts.iterator
      val expressions = args.iterator
      val buf = new StringBuffer(strings next())
      while(strings.hasNext) {
        buf append expressions.next
        buf append strings.next
      }
      logger debug buf.toString
    }
  }
}

/** Trait to add the logging functionality. Modify "log4j.properties" file to get your own settings */
trait Loggable {
  lazy val logger = Logger getLogger this.getClass

  /** Syntax sugar for "logger.debug(s)"
   * @param s smth to log */
  @deprecated("""Since Scala 2.10 use <log"hello world"> instead""", "2.10.0")
  def $(s: => Any) {
    logger debug s
  }
}
