package org.zalando.etcdwatcher

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class EtcdWatcherModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {

    implicit val system = ActorSystem("etcdWatcherActorSystem")

    bindActor[EtcdWatcherActor](EtcdWatcherActor.name)
    bindActor[EtcdMainActor](EtcdMainActor.name)
  }
}
