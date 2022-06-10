ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "vertx",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.0-RC6",
      "dev.zio" %% "zio-test" % "2.0.0-RC6" % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-vertx-server-zio" % "1.0.0-RC3",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.0.0-RC3",
      "ch.qos.logback" % "logback-classic" % "1.2.10" % Runtime,
      "com.softwaremill.sttp.shared" %% "fs2" % "1.3.4"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
