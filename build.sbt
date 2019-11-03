val tapirVersion  = "0.11.9"
val http4sVersion = "0.20.0"
val meowVersion   = "0.4.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "tapir-http4s-todo-mvc",
    version := "0.1",
    scalaVersion := "2.12.8",
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
    libraryDependencies ++=
      Seq(
        "com.softwaremill.tapir" %% "tapir-core"                     % tapirVersion,
        "com.softwaremill.tapir" %% "tapir-http4s-server"            % tapirVersion,
        "com.softwaremill.tapir" %% "tapir-json-circe"               % tapirVersion,
        "org.http4s"             %% "http4s-dsl"                     % http4sVersion,
        "org.http4s"             %% "http4s-server"                  % http4sVersion,
        "ch.qos.logback"         % "logback-classic"                 % "1.3.0-alpha4",
        "org.webjars"            % "swagger-ui"                      % "3.20.9",
        "com.softwaremill.tapir" %% "tapir-openapi-docs"             % tapirVersion,
        "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml"       % tapirVersion,
        "com.olegpy"             %% "meow-mtl-core"                  % meowVersion,
        "com.olegpy"             %% "meow-mtl-effects"               % meowVersion,
        "com.softwaremill.sttp"  %% "async-http-client-backend-cats" % "1.5.11" % Test,
        "com.softwaremill.tapir" %% "tapir-sttp-client"              % tapirVersion % Test,
        "org.scalatest"          %% "scalatest"                      % "3.0.5" % Test
      ),
    scalacOptions --= Seq("-Ywarn-value-discard")
  )
