package io.prediction.commons.settings

import io.prediction.commons.Spec

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class SystemInfosSpec extends Specification {
  def is = s2"""

  PredictionIO SystemInfos Specification

    SystemInfos can be implemented by:
    - MongoSystemInfos ${mongoSystemInfos}

  """

  def mongoSystemInfos = s2"""

    MongoSystemInfos should
    - behave like any SystemInfos implementation ${systemInfos(newMongoSystemInfos)}
    - (database cleanup) ${Step(Spec.mongoClient(mongoDbName).dropDatabase())}

  """

  def systemInfos(systemInfos: SystemInfos) = s2"""

    create and get a system info entry ${insertAndGet(systemInfos)}
    update a system info entry ${update(systemInfos)}
    delete a system info entry ${delete(systemInfos)}
    backup and restore system info entries ${backuprestore(systemInfos)}

  """

  val mongoDbName = "predictionio_mongosysteminfos_test"
  def newMongoSystemInfos = new mongodb.MongoSystemInfos(Spec.mongoClient(mongoDbName))

  def insertAndGet(systemInfos: SystemInfos) = {
    val version = SystemInfo(
      id = "version",
      value = "0.4-SNAPSHOT",
      description = Some("PredictionIO Version"))
    systemInfos.insert(version)
    systemInfos.get("version") must beSome(version).eventually
  }

  def update(systemInfos: SystemInfos) = {
    val build = SystemInfo(
      id = "build",
      value = "123",
      description = None)
    systemInfos.insert(build)
    val updatedBuild = build.copy(value = "124")
    systemInfos.update(updatedBuild)
    systemInfos.get("build") must beSome(updatedBuild)
  }

  def delete(systemInfos: SystemInfos) = {
    val foo = SystemInfo(
      id = "foo",
      value = "bar",
      description = None)
    systemInfos.insert(foo)
    systemInfos.delete("foo")
    systemInfos.get("foo") must beNone
  }

  def backuprestore(systemInfos: SystemInfos) = {
    val rev = SystemInfo(
      id = "rev",
      value = "321",
      description = Some("software revision"))
    systemInfos.insert(rev)
    val fn = "systeminfos.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(systemInfos.backup())
    } finally {
      fos.close()
    }
    systemInfos.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { data =>
      data must contain(rev)
    } getOrElse 1 === 2
  }
}
