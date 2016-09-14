package org.zalando.etcdwatcher

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, TestProbe }
import org.specs2.mock.Mockito
import org.specs2.mutable._

import scala.concurrent.duration._

class EtcdMainActorSpec extends Specification with Mockito {

  implicit private val system = ActorSystem.create("system")

  private class TestSetup {
    val watcherActorProbe = TestProbe()
    val watcherActorRef = watcherActorProbe.ref

    val configListener = mock[ConfigListener]
    val timeoutSettings = mock[TimeoutSettings]
    timeoutSettings.TimeoutErrorRetryTimeout returns 0.seconds
    timeoutSettings.UnexpectedErrorRetryTimeout returns 0.seconds

    val mainActorRef = TestActorRef(
      new EtcdMainActor(configListener, watcherActorRef, timeoutSettings)
    )
  }

  val key: String = "feature.enabled"
  val value: String = "ON"

  "Main actor" should {
    "should update state of dynamicConfSettings" in {
      val ts = new TestSetup()

      ts.mainActorRef.tell(UpdateKeys(Map(key -> value)), ts.watcherActorRef)
      there was one(ts.configListener).keyUpdated(key, value)
      ok
    }

    //    "should not set value if provided key does not exist" in {
    //      val ts = new TestSetup()
    //
    //      ts.mainActorRef.tell(UpdateKeys(Map("invalid-key" -> "ON")), ts.watcherActorRef)
    //      there was no(ts.dynamicConfSettings).setBooleanKey(any[DynamicKeyBoolean], anyBoolean)
    //      ok
    //    }
    //
    //    "should not set value if value incorrect" in {
    //      val ts = new TestSetup()
    //
    //      ts.mainActorRef.tell(UpdateKeys(Map("key-test" -> "incorrect-value")), ts.watcherActorRef)
    //      there was no(ts.dynamicConfSettings).setBooleanKey(any[DynamicKeyBoolean], anyBoolean)
    //      ok
    //    }
    //
    "should schedule child again if timeout expired" in {
      val ts = new TestSetup()

      ts.watcherActorProbe.expectMsg(RetrieveKeys) // this message occurs on main actor startup
      val exception = new TimeoutException("Connection interrupted by timeout")
      ts.mainActorRef.tell(HandleFailure(exception), ts.watcherActorRef)
      ts.watcherActorProbe.expectMsg(RetrieveKeys)
      ok
    }

    "should schedule child again if unexpected error happened" in {
      val ts = new TestSetup()

      ts.watcherActorProbe.expectMsg(RetrieveKeys) // this message occurs on main actor startup
      val exception = new Exception("Something went wrong")
      ts.mainActorRef.tell(HandleFailure(exception), ts.watcherActorRef)
      ts.watcherActorProbe.expectMsg(RetrieveKeys)
      ok
    }
  }
}
