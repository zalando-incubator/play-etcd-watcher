package org.zalando.etcdwatcher

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, TestProbe }
import mockws.MockWS
import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.Configuration
import play.api.mvc.Action
import play.api.mvc.Results._

class EtcdWatcherActorSpec extends Specification with Mockito {

  implicit private val system = ActorSystem.create("system")

  private val configMock = mock[Configuration]
  private val EtcdUrl: String = "http://etcd-url"
  private val EtcdDir: String = "local"
  configMock.getString("etcd.directory") returns Some(EtcdDir)
  configMock.getString("etcd.url") returns Some(EtcdUrl)

  val key1 = "feature1.enabled"
  val key2 = "feature2.enabled"

  "Watcher actor" should {
    "should watch and tell main actor if the key was updated " in {
      val mainProbe: TestProbe = TestProbe.apply()

      val value = "ON"
      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action { Ok(s"""{"action":"set","node":{"key":"/$EtcdDir/$key1","value":"$value"}}""") }
      }

      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsg(UpdateKeys(Map(key1 -> value)))
      ok
    }

    "should ask main actor to handle failure" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action { GatewayTimeout }
      }

      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsgType[HandleFailure]
      ok
    }

    "should tell main actor to update values" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action {
            Ok(
              s"""
              |{"action":"get","node": {"key":"$EtcdDir","dir":true,"nodes":[
              |{"key":"/$EtcdDir/$key1","value":"ON"},
              |{"key":"/$EtcdDir/$key2","value":"OFF"}
              |]}}
            """.stripMargin
            )
          }
      }

      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(RetrieveKeys, mainProbe.ref)
      mainProbe.expectMsg(UpdateKeys(
        Map(
          key1 -> "ON",
          key2 -> "OFF"
        )
      ))
      ok
    }
  }
}
