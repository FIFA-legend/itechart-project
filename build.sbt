enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
mainClass in Compile := Some("com.itechart.http.GuessServer")

name := "CourseProject"

version := "0.1"

scalaVersion := "2.13.6"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Ymacro-annotations",
  "-Xfatal-warnings"
)

ThisBuild / scalafmtOnCompile := true

val http4sVersion           = "0.23.1"
val circeVersion            = "0.14.1"
val circeConfigVersion      = "0.8.0"
val doobieVersion           = "1.0.0-M2"
val catsVersion             = "2.6.1"
val catsTaglessVersion      = "0.14.0"
val catsEffectVersion       = "3.2.9"
val epimetheusVersion       = "0.6.0-M2"
val catsScalacheckVersion   = "0.3.1"
val log4CatsVersion         = "1.1.1"
val scalaTestVersion        = "3.1.0.0-RC2"
val h2Version               = "1.4.200"
val enumeratumVersion       = "1.7.0"
val dtoMapperChimneyVersion = "0.6.1"
val cirisVersion            = "2.2.0"

libraryDependencies ++= Seq(
  "org.typelevel"            %% "cats-core"                     % catsVersion,
  "org.typelevel"            %% "cats-effect"                   % catsEffectVersion,
  "org.typelevel"            %% "cats-tagless-macros"           % catsTaglessVersion,
  "org.typelevel"            %% "simulacrum"                    % "1.0.1",
  "org.typelevel"            %% "squants"                       % "1.8.3",
  "org.typelevel"            %% "log4cats-slf4j"                % "2.1.1",
  "org.http4s"               %% "http4s-dsl"                    % http4sVersion,
  "org.http4s"               %% "http4s-blaze-server"           % http4sVersion,
  "org.http4s"               %% "http4s-blaze-client"           % http4sVersion,
  "org.http4s"               %% "http4s-circe"                  % http4sVersion,
  "org.http4s"               %% "http4s-jdk-http-client"        % "0.5.0",
  "dev.profunktor"           %% "http4s-jwt-auth"               % "1.0.0",
  "dev.profunktor"           %% "redis4cats-log4cats"           % "1.0.0",
  "dev.profunktor"           %% "redis4cats-effects"            % "1.0.0",
  "ch.qos.logback"            % "logback-classic"               % "1.2.6",
  "org.tpolecat"             %% "doobie-core"                   % doobieVersion,
  "org.tpolecat"             %% "doobie-h2"                     % doobieVersion,
  "org.tpolecat"             %% "doobie-hikari"                 % doobieVersion,
  "org.tpolecat"             %% "doobie-scalatest"              % doobieVersion         % Test,
  "com.h2database"            % "h2"                            % "1.4.200",
  "mysql"                     % "mysql-connector-java"          % "8.0.20",
  "org.flywaydb"              % "flyway-core"                   % "6.2.4",
  "io.chrisdavenport"        %% "cats-scalacheck"               % catsScalacheckVersion % Test,
  "io.chrisdavenport"        %% "epimetheus-http4s"             % epimetheusVersion,
  "io.circe"                 %% "circe-core"                    % circeVersion,
  "io.circe"                 %% "circe-generic"                 % circeVersion,
  "io.circe"                 %% "circe-generic-extras"          % circeVersion,
  "io.circe"                 %% "circe-optics"                  % circeVersion,
  "io.circe"                 %% "circe-parser"                  % circeVersion,
  "io.circe"                 %% "circe-config"                  % circeConfigVersion,
  "is.cir"                   %% "ciris"                         % cirisVersion,
  "is.cir"                   %% "ciris-enumeratum"              % cirisVersion,
  "is.cir"                   %% "ciris-refined"                 % cirisVersion,
  "com.beachape"             %% "enumeratum"                    % enumeratumVersion,
  "com.beachape"             %% "enumeratum-circe"              % enumeratumVersion,
  "io.scalaland"             %% "chimney"                       % dtoMapperChimneyVersion,
  "eu.timepit"               %% "refined"                       % "0.9.27",
  "com.github.daddykotex"    %% "courier"                       % "3.0.1",
  "org.scalatestplus"        %% "scalatestplus-scalacheck"      % scalaTestVersion      % Test,
  "org.scalatestplus"        %% "selenium-2-45"                 % scalaTestVersion      % Test,
  "org.scalatest"            %% "scalatest"                     % "3.0.8"               % Test,
  "com.codecommit"           %% "cats-effect-testing-scalatest" % "0.4.1"               % Test,
  "org.scalaj"               %% "scalaj-http"                   % "2.4.2"               % Test,
  "org.mockito"              %% "mockito-scala"                 % "1.16.46"             % Test,
  "org.tpolecat"             %% "atto-core"                     % "0.9.5",
  "org.slf4j"                 % "slf4j-nop"                     % "1.7.32",
  "org.fusesource.leveldbjni" % "leveldbjni-all"                % "1.8",
  "com.github.pureconfig"    %% "pureconfig"                    % "0.17.0",
  "tf.tofu"                  %% "derevo-core"                   % "0.12.6",
  "io.estatico"              %% "newtype"                       % "0.4.4"
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
)

run / fork := true
