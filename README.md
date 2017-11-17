[![Build Status](https://travis-ci.org/zalando-incubator/play-etcd-watcher.svg?branch=master)](https://travis-ci.org/zalando-incubator/play-etcd-watcher)
[![Coverage Status](https://coveralls.io/repos/github/zalando-incubator/play-etcd-watcher/badge.svg?branch=master)](https://coveralls.io/github/zalando-incubator/play-etcd-watcher?branch=master)
[![codecov.io](http://codecov.io/github/zalando-incubator/play-etcd-watcher/coverage.svg?branch=master)](http://codecov.io/github/zalando-incubator/play-etcd-watcher?branch=master)
[![MIT licensed](https://img.shields.io/badge/license-MIT-green.svg)](https://raw.githubusercontent.com/hyperium/hyper/master/LICENSE)

# Etcd Watcher for Play

## What is play-etcd-watcher?
play-etcd-watcher is a library to listen for changes in
[etcd](https://github.com/coreos/etcd) via its REST interface and notify the client
application through its listeners.

## Target users
You develop a Scala/Play service and at some point you need to change configuration
parameters, e.g. feature toggle. Would you redeploy your application with a new
parameter? Or maybe you change the source code? If this sounds familiar but you
want to avoid such hassles, then you probably need some kind of configuration service,
e.g. key-value store. If you use etcd, then ```play-etcd-watcher``` will help to listen
for key changes in your application.

## Key features
The updates are received by the client application instantaneous due to
the implemented long polling. After an error, the connection is tried
again after the timeout (defaults to 10 seconds). In normal operation the
connection is re-established after a period of time (defaults to 90
seconds).

## Getting started
Add play-etcd-watcher dependency to your ```build.sbt```:

```scala
libraryDependencies += Seq(
  "org.zalando" %% "play-etcd-watcher" % "2.6.0"
)
```

Add the module to ```conf/application.conf```:
```scala
play.modules.enabled += "org.zalando.etcdwatcher.EtcdWatcherModule"
```

and specify the following config parameters:
```scala
etcd {
  url = http://my-etcd.url
  directory = /my/etcd/directory
}
```

where ```etcd.url``` denotes the etcd url and ```etcd.directory```
denotes the directory being watched. The directory will be scanned
recursively.

Next, implement the listener trait:

```scala
trait ConfigListener {
  def keysUpdated(keyValues: Map[String, Option[String]]): Unit
}
```
and bind it to the implementation in your Module, e.g.
```scala
class AppModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    ...
    bind[ConfigListener].to[MyConfigListener]
  }
}
```
The method will be called on startup and on each key change. The
```Option``` value of ```None``` denotes the deletion of the key.

## Versioning
The version number follows the Play version but does not necessarily
correspond to its minor version. As an example, if the first release of `play-etcd-watcher`
was made for Play ```2.5.4```, the version would be ```2.5.0```.

## Developer notes
### Releasing artifacts to nexus

#### Prerequisites
To deploy the artifact to nexus you need to have the following prerequisites:
 - Sonatype account
 - Rights for the corresponding repository
 - Your pgp key to sign the artifact

#### Deployment procedure

See [this page](http://central.sonatype.org/pages/ossrh-guide.html) for more documentation.
We use `sbt-sonatype` and `sbt-pgp` plugins to take care of deployment.
To deploy, run the following commands:
   git clean -df
   sbt publishSigned
   sbt sonatypeRelease

IF the last step fails with an error like this:

    [error] Multiple repositories are found:
    [error] [orgzalando-1999] status:open, profile:org.zalando(6cc4d987a65f6) description: Implicitly created (auto staging).
    [error] [orgzalando-2009] status:open, profile:org.zalando(6cc4d987a65f6) description: Implicitly created (auto staging).
    [error] Specify one of the repository ids in the command line

then you may need to specify the id explicitly:

   sbt "sonatypeRelease orgzalando-2009"

Most probably `profileId` is that the last in the list but check
[staging repositories](https://oss.sonatype.org/#stagingRepositories) (you need to be logged in).

Have fun!

## License

The MIT License (MIT)

Copyright (c) 2016 Zalando SE

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
