package org.zalando.etcdwatcher

import akka.actor.ActorSystem
import akka.testkit.{ TestActorRef, TestProbe }
import mockws.MockWS
import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.mvc.Results._

import scala.concurrent.duration._

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
    "watch and tell main actor if the key was updated" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val value = "ON"
      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action { Ok(s"""{"action":"set","node":{"key":"/$EtcdDir/$key1","value":"$value"}}""") }
      }

      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsg(UpdateKeys(Map(key1 -> Some(value))))
      ok
    }

    "ask main actor to handle failure if the directory is not found" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action { NotFound(s"""{"errorCode":100,"message":"Key not found","cause":"/$EtcdDir","index":19}""") }
      }

      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsgType[HandleFailure]
      ok
    }

    "ask main actor to handle failure" in {
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

    "handle failure if unexpected problem occurs" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) => Action { _ => throw new RuntimeException }
      }

      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsgType[HandleFailure]
      ok
    }

    "tell main actor to update values" in {
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
          key1 -> Some("ON"),
          key2 -> Some("OFF")
        )
      ))
      ok
    }

    "ignore if the actor message is of unknown type" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = mock[WSClient]
      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell("Some unrecognized format of actor message", mainProbe.ref)
      mainProbe.expectNoMsg(1.second)
      ok
    }

    "update when directory is empty" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action {
            Ok(
              s"""{"action":"get","node":{"key":"/$EtcdDir","dir":true,"modifiedIndex":22,"createdIndex":22}}"""
            )
          }
      }
      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(RetrieveKeys, mainProbe.ref)
      mainProbe.expectMsg(UpdateKeys(Map.empty))
      ok
    }

    "notify if key is deleted" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val value = "ON"

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action {
            Ok(
              s"""{"action":"delete",
                 |"node":{"key":"/$EtcdDir/$key1","modifiedIndex":25,"createdIndex":24},
                 |"prevNode":{"key":"/$EtcdDir/$key1","value":"$value","modifiedIndex":24,"createdIndex":24}}""".stripMargin
            )
          }
      }
      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsg(UpdateKeys(Map(key1 -> None)))
      ok
    }

    "return empty result if directory not found" in {
      val mainProbe: TestProbe = TestProbe.apply()

      val value = "ON"

      val url = s"$EtcdUrl/v2/keys/$EtcdDir/?wait=true&recursive=true"
      val ws = MockWS {
        case ("GET", `url`) =>
          Action {
            NotFound(
              s"""{"errorCode":100,"message":"Key not found","cause":"/key","index":178}"""
            )
          }
      }
      val watcherRef = TestActorRef(new EtcdWatcherActor(ws, configMock))
      watcherRef.tell(WatchKeys, mainProbe.ref)
      mainProbe.expectMsg(UpdateKeys(Map()))
      ok
    }
  }
}
