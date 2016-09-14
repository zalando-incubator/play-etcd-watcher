package org.zalando.etcdwatcher

trait ConfigListener {
  def keyUpdated(key: String, value: String): Unit
}
