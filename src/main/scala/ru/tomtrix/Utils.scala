package ru.tomtrix

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}

/**
 *
 */
object Utils {
  def serialize(obj: Serializable) = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos writeObject(obj)
    val result = baos toByteArray()
    oos close()
    baos close()
    result
  }

  def deserialize(buf: Array[Byte]) = {
    val bais = new ByteArrayInputStream(buf)
    val ois = new ObjectInputStream(bais)
    val result = ois.readObject().asInstanceOf[Serializable]
    ois close()
    bais close()
    result
  }
}
