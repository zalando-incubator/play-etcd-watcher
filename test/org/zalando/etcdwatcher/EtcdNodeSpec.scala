package org.zalando.etcdwatcher

import org.specs2.mutable._

class EtcdNodeSpec extends Specification {

  val key: String = "feature.enabled"
  val value: String = "ON"

  "EtcdNode.flatten()" should {
    "work correctly" in {
      val value1 = EtcdValueNode("/dir/dir1/key1", Some("value1"))
      val subdir1 = EtcdDirNode("/dir/dir1", List(value1))

      val value2 = EtcdValueNode("/dir/dir2/key2", Some("value2"))
      val subdir2 = EtcdDirNode("/dir/dir2", List(value2))

      val dirNode = EtcdDirNode("/dir", List(subdir1, subdir2))
      dirNode.flatten() should containAllOf(List(value1, value2))
    }

    "work correctly with same-level values and directories" in {
      val value1 = EtcdValueNode("/dir1/dir2/dir3/key1", Some("value1"))
      val value2 = EtcdValueNode("/dir1/dir2/dir3/key2", Some("value2"))
      val subsubdir = EtcdDirNode("/dir1/dir2/dir3", List(value1, value2))
      val value3 = EtcdValueNode("/dir1/dir2/key3", Some("value3"))
      val subdir = EtcdDirNode("/dir1/dir2", List(subsubdir, value3))
      val dirNode = EtcdDirNode("/dir1", List(subdir))

      dirNode.flatten() should haveSize(3)
      dirNode.flatten() should containAllOf(List(value1, value2, value3))
    }
  }
}
