val tapirVersion  = "0.0.7"
val http4sVersion = "0.20.0-M4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "tapir-http4s-todo-mvc",
    version := "0.1",
    scalaVersion := "2.12.8",
    libraryDependencies ++=
      Seq(
        "com.softwaremill.tapir" %% "tapir-core"                     % tapirVersion,
        "com.softwaremill.tapir" %% "tapir-http4s-server"            % tapirVersion,
        "com.softwaremill.tapir" %% "tapir-json-circe"               % tapirVersion,
        "org.http4s"             %% "http4s-dsl"                     % http4sVersion,
        "org.http4s"             %% "http4s-server"                  % http4sVersion,
        "ch.qos.logback"         % "logback-classic"                 % "1.3.0-alpha4",
        "com.softwaremill.tapir" %% "tapir-sttp-client"              % tapirVersion % Test,
        "com.softwaremill.sttp"  %% "async-http-client-backend-cats" % "1.5.2" % Test,
        "org.scalatest"          %% "scalatest"                      % "3.0.5" % Test
      )
  )
