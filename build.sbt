ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

lazy val akkaVersion = "2.8.0"
lazy val akkaHttpVersion = "10.5.0"
lazy val akkaGrpcVersion = "2.8.0"


lazy val global = (project in file("."))
  .settings(
    name := "PaperIO"
  ).aggregate(
    module_geometry,
    module_game_logic,
    module_singleplayer,
    module_multiplayer,
    module_server,
    module_ml,
    module_desktop
  )

lazy val module_geometry = project
  .settings(
    name := "module_geometry",
    libraryDependencies ++= Seq(
      "org.locationtech.jts" % "jts-core" % "1.19.0",
      "org.scalactic" %% "scalactic" % "3.2.15",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test"
    )
  )

lazy val module_game_logic = project
  .settings(
    name := "module_game_logic",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.scalactic" %% "scalactic" % "3.2.15",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test"
    )
  )
  .dependsOn(module_geometry)

lazy val module_ml = project
  .settings(
    name := "module_ml",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",
      "com.microsoft.onnxruntime" % "onnxruntime" % "1.14.0",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test"
    )
  )
  .dependsOn(module_game_logic)

lazy val module_server = project
  .settings(
    name := "module_server",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-pki" % akkaVersion,

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,

      "ch.qos.logback" % "logback-classic" % "1.4.6",

      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    ),
    akkaGrpcCodeGeneratorSettings += "server_power_apis"
  )
  .enablePlugins(AkkaGrpcPlugin)
  .dependsOn(module_game_logic)
  .dependsOn(module_ml)

lazy val module_singleplayer = project
  .settings(
    name := "module_singleplayer",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-freetype" % "1.11.0"
    )
  )
  .dependsOn(module_game_logic)
  .dependsOn(module_ml)

lazy val module_multiplayer = project
  .settings(
    name := "module_multiplayer",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-freetype" % "1.11.0",

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-pki" % akkaVersion,

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion
    ),
    akkaGrpcCodeGeneratorSettings += "server_power_apis"
  )
  .enablePlugins(AkkaGrpcPlugin)
  .dependsOn(module_game_logic)

lazy val module_desktop = project
  .settings(
    name := "module_desktop",
    libraryDependencies ++= Seq(
      "com.badlogicgames.gdx" % "gdx" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-backend-lwjgl" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-platform" % "1.11.0" classifier "natives-desktop",
      "com.badlogicgames.gdx" % "gdx-freetype" % "1.11.0",
      "com.badlogicgames.gdx" % "gdx-freetype-platform" % "1.11.0" classifier "natives-desktop"
    )
  )
  .dependsOn(module_singleplayer)
  .dependsOn(module_multiplayer)