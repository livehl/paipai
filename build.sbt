import com.typesafe.sbt.packager.docker._
import NativePackagerHelper._

name := "paipai"

version := "1.0.2"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.apache.httpcomponents" % "httpmime" % "4.3.5",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5",
  "mysql" % "mysql-connector-java" % "5.1.31",
  "commons-dbutils" % "commons-dbutils" % "1.5",
  "commons-collections" % "commons-collections" % "3.2.2",
  "org.javassist" % "javassist" % "3.17.1-GA",
  "commons-dbcp" % "commons-dbcp" % "1.4",
  "javax.mail" % "mail" % "1.4.7",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.3",
  "net.jpountz.lz4" % "lz4" % "1.3.0",
  "com.typesafe" % "config" % "1.3.0",
  "io.swagger" % "swagger-annotations" % "1.5.6"
)

resolvers := Seq(Resolver.defaultLocal,"handuser" at "http://sbt.handuser.com/maven2/")++resolvers.value

val root = (project in file(".")).enablePlugins(DockerPlugin).enablePlugins(JavaAppPackaging)


doc in Compile <<= target.map(_ / "none")

dockerBaseImage := "anapsix/alpine-java:jre8"

dockerCommands :=dockerCommands.value.take(3) ++ Seq(
  ExecCmd("RUN","ln", "-sf","/usr/share/zoneinfo/Asia/Shanghai", "/etc/localtime"),
  ExecCmd("RUN","echo","\\\"Asia/Shanghai\\\"",">","/etc/timezone")
)++ dockerCommands.value.drop(3)

packageName in Docker := packageName.value

dockerRepository :=Some("registry.aliyuncs.com/zx")
