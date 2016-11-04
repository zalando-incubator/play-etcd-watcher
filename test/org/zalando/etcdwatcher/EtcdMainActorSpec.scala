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
    val timeoutSettings = spy(new TimeoutSettings)
    timeoutSettings.UnexpectedErrorRetryTimeout returns 2.seconds

    val mainActorRef = TestActorRef(
      new EtcdMainActor(configListener, watcherActorRef, timeoutSettings)
    )
  }

  val key: String = "feature.enabled"
  val value: String = "ON"

  "Main actor" should {
    "update state of a configListener" in {
      val ts = new TestSetup()

      ts.mainActorRef.tell(UpdateKeys(Map(key -> Some(value))), ts.watcherActorRef)
      there was one(ts.configListener).keysUpdated(Map(key -> Some(value)))
      ok
    }

    "schedule child again if server times out" in {
      val ts = new TestSetup()

      ts.watcherActorProbe.expectMsg(RetrieveKeys) // this message occurs on main actor startup
      val exception = new TimeoutException("Connection interrupted by timeout")
      ts.mainActorRef.tell(HandleFailure(exception), ts.watcherActorRef)
      ts.watcherActorProbe.expectMsg(RetrieveKeys)
      ok
    }

    "schedule child again if unexpected error happened" in {
      val ts = new TestSetup()

      ts.watcherActorProbe.expectMsg(RetrieveKeys) // this message occurs on main actor startup
      val exception = new Exception("Something went wrong")
      ts.mainActorRef.tell(HandleFailure(exception), ts.watcherActorRef)
      ts.watcherActorProbe.expectNoMsg(1.second)
      ts.watcherActorProbe.expectMsg(RetrieveKeys)
      ok
    }

    "ignore if the actor message is of unknown type" in {
      val ts = new TestSetup()

      ts.watcherActorProbe.expectMsg(RetrieveKeys) // this message occurs on main actor startup
      ts.mainActorRef.tell("Some unrecognized format of actor message", ts.watcherActorRef)
      ts.watcherActorProbe.expectNoMsg(1.second)
      ok
    }
  }
}
