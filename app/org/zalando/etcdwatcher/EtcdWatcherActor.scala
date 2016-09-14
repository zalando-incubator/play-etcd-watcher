package org.zalando.etcdwatcher

import javax.inject.Inject

import akka.actor.{ Actor, ActorLogging, ActorRef, _ }
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.{ WSClient, WSResponse }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

object EtcdWatcherActor {
  final val name = "etcd-watcher"
}

class EtcdWatcherActor @Inject() (ws: WSClient, config: Configuration) extends Actor with ActorLogging {

  private[this] val directory = config.getString("etcd.directory").getOrElse {
    throw new RuntimeException("Etcd directory not set")
  }

  private[this] val serverUrl = config.getString("etcd.url").getOrElse {
    throw new RuntimeException("Etcd url not set")
  }

  private implicit val keyReads = new Reads[Key] {
    override def reads(js: JsValue): JsResult[Key] = {
      JsSuccess(
        Key(
          // keys come in the format "directory/key"
          // and we need to remove the leading directory path
          (js \ "key").as[String].drop(directory.length + 2),
          (js \ "value").as[String]
        )
      )
    }
  }

  override def receive = {
    case WatchKeys =>
      log.info(s"Connecting to watch keys in directory $directory")
      val senderActor = sender()
      ws.url(s"$serverUrl/v2/keys/$directory/?wait=true&recursive=true")
        .withHeaders("Accept" -> "application/json")
        .withRequestTimeout(90.seconds)
        .get()
        .onComplete(onCompleteAction(_, senderActor, nested = false))

    case RetrieveKeys =>
      log.info("Retrieving keys from etcd")
      val senderActor = sender()
      ws.url(s"$serverUrl/v2/keys/$directory/?recursive=true")
        .withHeaders("Accept" -> "application/json")
        .get()
        .onComplete(onCompleteAction(_, senderActor, nested = true))

    case _ => log.warning("Unknown message received")
  }

  private def onCompleteAction(tryResponse: Try[WSResponse], senderActor: ActorRef, nested: Boolean): Unit = {
    val message = tryResponse match {
      case Success(response) =>
        Try(buildKeyMap(response.json, nested)) match {
          case Success(Some(keys)) => UpdateKeys(keys)
          case Success(None) => UpdateKeys(Map.empty)
          case Failure(exc) => HandleFailure(exc)
        }
      case Failure(exc) => HandleFailure(exc)
    }
    senderActor ! message
  }

  // TODO nested?
  private def buildKeyMap(json: JsValue, nested: Boolean): Option[Map[String, String]] = {

    val result: Option[Seq[Key]] = if (nested) {
      (json \ "node" \ "nodes").asOpt[Seq[Key]]
    } else {
      (json \ "node").asOpt[Key].map(Seq(_))
    }
    result.map(_.map(_.asTuple).toMap)
  }
}
