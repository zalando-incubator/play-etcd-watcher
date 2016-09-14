package org.zalando.etcdwatcher

import java.util.concurrent.TimeoutException
import javax.inject.{ Inject, Named, Singleton }

import akka.actor.{ Actor, ActorLogging, ActorRef }
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class TimeoutSettings() {
  val UnexpectedErrorRetryTimeout = 10.seconds
  val TimeoutErrorRetryTimeout = 0.seconds
}

object EtcdMainActor {
  final val name = "etcd-main"
}

class EtcdMainActor @Inject() (
    configListener: ConfigListener,
    @Named(EtcdWatcherActor.name) watcherActor: ActorRef,
    timeoutSettings: TimeoutSettings
) extends Actor with InjectedActorSupport with ActorLogging {

  override def preStart(): Unit = {
    watcherActor ! RetrieveKeys
  }

  override def receive = {
    case UpdateKeys(keys) =>
      keys.foreach {
        case (rawKey: String, rawValue: String) =>
          log.info(s"Received updated key [$rawKey] with value [$rawValue]")
          configListener.keyUpdated(rawKey, rawValue)
      }
      watcherActor ! WatchKeys

    case HandleFailure(exception) =>
      val retryTimeout = exception match {
        case exc: TimeoutException =>
          log.info(s"Disconnected by timeout [${exc.getMessage}]")
          timeoutSettings.TimeoutErrorRetryTimeout
        case err =>
          log.error(err, s"Unexpected error while requesting key [${err.getMessage}]")
          timeoutSettings.TimeoutErrorRetryTimeout
      }
      context.system.scheduler.scheduleOnce(retryTimeout, watcherActor, RetrieveKeys)

    case _ => log.warning("Unknown message received")
  }
}
