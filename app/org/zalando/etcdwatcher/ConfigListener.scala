package org.zalando.etcdwatcher

trait ConfigListener {
  def keysUpdated(keyValues: Map[String, Option[String]]): Unit
}
