package ru.tomtrix.synch

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import org.apache.log4j.Logger

/** Log4j apache logger*/
object ApacheLogger {
  /** Instance of a logger */
  lazy val logger = Logger getLogger this.getClass

  /** Syntax sugar for "logger.debug(s)"
    * @param s smth to log */
  @deprecated("""Since Scala 2.10 use <log"hello world"> instead""", "2.10.0")
  def $(s: => Any) {
    logger debug s
  }

  /** String interpolation feature */
  implicit class LogHelper(val sc: StringContext) extends AnyVal {
    /** Syntax sugar for "logger.debug(s)"
     * @since 2.10 */
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
