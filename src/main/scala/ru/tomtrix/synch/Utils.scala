package ru.tomtrix.synch

import java.io._

/** Log4j apache logger*/
object ApacheLogger extends Loggable {
  /** String interpolation feature */
  implicit class LogHelper(val sc: StringContext) extends AnyVal {
    /** Syntax sugar for "logger.debug(s)"
     * @since 2.10
     * @example {{{ log"My age is ${h -1}" }}}*/
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

/** Basic Java-based serializer */
object Serializer {
  /** Serializes an object into a byte array
   * @param obj serializable object
   * @return byte array of a serialized object */
  def serialize(obj: Serializable) = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos writeObject(obj)
    val result = baos toByteArray()
    oos close()
    baos close()
    result
  }

  /** Deserializes an object from a byte array
   * @param buf byte array that keeps an object
   * @return deserialized object */
  def deserialize[T <% Serializable](buf: Array[Byte]) = {
    val bais = new ByteArrayInputStream(buf)
    val ois = new ObjectInputStream(bais)
    val result = ois.readObject().asInstanceOf[T]
    ois close()
    bais close()
    result
  }
}

object SafeCode extends Loggable {
  def safe[T](func: => T, finallyFunc: => Unit = {}, log: Boolean = true): Option[T] = {
    try {
      Some(func)
    }
    catch {
      case e: Throwable => if (log) logger error("SafeCode error", e)
      None
    }
    finally {
      finallyFunc
    }
  }

  def safe$[T](func: => T, finallyFunc: => Unit = {}): Option[T] = safe(func, finallyFunc, log = false)
}