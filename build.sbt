val tapirVersion  = "1.2.7"
val http4sVersion = "0.23.18"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "tapir-http4s-todo-mvc",
    version      := "0.1",
    scalaVersion := "3.2.1",
    libraryDependencies ++=
      Seq(
        "com.softwaremill.sttp.tapir" %% "tapir-core"              % tapirVersion,
        "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
        "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,
        "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
        "org.http4s"                  %% "http4s-core"             % http4sVersion,
        "org.http4s"                  %% "http4s-dsl"              % http4sVersion,
        "org.http4s"                  %% "http4s-ember-server"     % http4sVersion,
        "org.slf4j"                    % "slf4j-simple"            % "1.7.36"
      ),
    scalacOptions --= Seq("-Ywarn-value-discard")
  )
