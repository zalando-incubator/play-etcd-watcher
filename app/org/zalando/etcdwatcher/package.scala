package org.zalando

package object etcdwatcher {

  case class Key(key: String, value: String) {
    val asTuple: (String, String) = (key, value)
  }

  case class KeyNotSetException(message: String) extends Exception

  case object WatchKeys
  case object RetrieveKeys
  case class UpdateKeys(keys: Map[String, String])
  case class HandleFailure(exception: Throwable)

}
