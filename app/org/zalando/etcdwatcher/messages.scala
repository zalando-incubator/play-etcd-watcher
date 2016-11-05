package org.zalando.etcdwatcher

case object WatchKeys
case object RetrieveKeys
case class UpdateKeys(keys: Map[String, Option[String]])
case class HandleFailure(exception: Throwable)
