package org.zalando.etcdwatcher

import javax.inject.Inject

import akka.actor.{ Actor, ActorLogging, ActorRef }
import play.api.Configuration
import play.api.http.Status
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
          (js \ "value").asOpt[String]
        )
      )
    }
  }

  private val SingleNodeParser: (JsValue) => Option[Seq[Key]] = json => {
    (json \ "node").asOpt[Key].map(Seq(_))
  }
  private val NestedNodeParser: (JsValue) => Option[Seq[Key]] = json => {
    (json \ "node" \ "nodes").asOpt[Seq[Key]]
  }

  override def receive = {
    case WatchKeys =>
      log.info(s"Connecting to watch keys in directory $directory")
      val senderActor = sender()
      ws.url(s"$serverUrl/v2/keys/$directory/?wait=true&recursive=true")
        .withHeaders("Accept" -> "application/json")
        .withRequestTimeout(90.seconds)
        .get()
        .onComplete(onCompleteAction(_, senderActor, SingleNodeParser))

    case RetrieveKeys =>
      log.info("Retrieving keys from etcd")
      val senderActor = sender()
      ws.url(s"$serverUrl/v2/keys/$directory/?recursive=true")
        .withHeaders("Accept" -> "application/json")
        .get()
        .onComplete(onCompleteAction(_, senderActor, NestedNodeParser))

    case _ => log.warning("Unknown message received")
  }

  private def onCompleteAction(tryResponse: Try[WSResponse], senderActor: ActorRef,
    parse: JsValue => Option[Seq[Key]]): Unit = {

    def flattenInput(input: Seq[Key]): Map[String, Option[String]] = input.map(_.asTuple).toMap

    val message = tryResponse.flatMap { response =>
      response.status match {
        case Status.OK =>
          val result = parse(response.json).map(flattenInput)
          Success(result)
        case Status.NOT_FOUND =>
          log.warning(s"Directory $directory not found. Returning empty result")
          Success(Some(Map[String, Option[String]]()))
        case _ =>
          Failure(new RuntimeException(s"Returned status ${response.statusText} with body ${response.body}"))
      }
    } match {
      case Success(keysOpt) => UpdateKeys(keysOpt.getOrElse(Map.empty))
      case Failure(exc) => HandleFailure(exc)
    }

    senderActor ! message
  }
}
