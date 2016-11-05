package org.zalando.etcdwatcher

sealed trait EtcdNode {
  def flatten(acc: List[EtcdValueNode] = List()): List[EtcdValueNode] = {
    this match {
      case s: EtcdValueNode => s :: acc
      case EtcdDirNode(key, nodes) => acc ::: nodes.flatMap(_.flatten() ::: acc)
    }
  }
}

case class EtcdValueNode(key: String, value: Option[String]) extends EtcdNode {
  val asTuple: (String, Option[String]) = (key, value)
}
case class EtcdDirNode(key: String, nodes: List[EtcdNode]) extends EtcdNode
