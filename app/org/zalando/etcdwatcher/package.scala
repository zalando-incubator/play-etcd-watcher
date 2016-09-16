package org.zalando

package object etcdwatcher {

  case class Key(key: String, value: Option[String]) {
    val asTuple: (String, Option[String]) = (key, value)
  }

  case class KeyNotSetException(message: String) extends Exception

  case object WatchKeys
  case object RetrieveKeys
  case class UpdateKeys(keys: Map[String, Option[String]])
  case class HandleFailure(exception: Throwable)

}
