package org.zalando.etcdwatcher

sealed trait EtcdNode {
  def flatten(): Seq[EtcdValueNode] = {
    this match {
      case s: EtcdValueNode => Seq(s)
      case EtcdDirNode(key, nodes) => nodes.flatMap(_.flatten())
    }
  }
}

case class EtcdValueNode(key: String, value: Option[String]) extends EtcdNode {
  val asTuple: (String, Option[String]) = (key, value)
}
case class EtcdDirNode(key: String, nodes: Seq[EtcdNode]) extends EtcdNode
