package com.itechart.project

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import com.itechart.project.configuration.ConfigurationTypes.AppConfiguration
import com.itechart.project.context.AppContext
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.config.parser
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    serverResource[IO]
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  private def serverResource[F[_]: ContextShift: ConcurrentEffect: Timer: Logger]: Resource[F, Server[F]] = {
    for {
      configuration <- Resource.eval(parser.decodePathF[F, AppConfiguration]("app"))
      httpApp       <- AppContext.setUp[F](configuration)

      server <- BlazeServerBuilder[F](ExecutionContext.global)
        .bindHttp(configuration.server.port, configuration.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server
  }
}
