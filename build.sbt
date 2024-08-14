val ScalatraVersion = "3.1.0"

ThisBuild / scalaVersion := "2.12.19"
ThisBuild / organization := "br.com.oystr"

lazy val hello = (project in file("."))
  .settings(
    name := "hotel-test",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "org.scalatra" %% "scalatra-jakarta" % ScalatraVersion,
      "org.scalatra" %% "scalatra-scalatest-jakarta" % ScalatraVersion % "test",
      "org.eclipse.jetty.ee10" % "jetty-ee10-webapp" % "12.0.11" % "container",
      "jakarta.servlet" % "jakarta.servlet-api" % "6.1.0" % "provided",
      "org.scalatra" %% "scalatra-json-jakarta" % ScalatraVersion,
      "org.json4s"   %% "json4s-jackson" % "4.0.7",
      "org.scalatra" %% "scalatra-forms-jakarta" % ScalatraVersion,
      "com.typesafe.slick" %% "slick" % "3.5.1",
      "com.h2database" % "h2" % "2.2.224",
      "org.scalatra" %% "scalatra-swagger-jakarta" % "3.1.0"),
  )

enablePlugins(SbtTwirl)
enablePlugins(JettyPlugin)

Jetty / containerLibs := Seq("org.eclipse.jetty.ee10" % "jetty-ee10-runner" % "12.0.11" intransitive())
Jetty / containerMain := "org.eclipse.jetty.ee10.runner.Runner"


