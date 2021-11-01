package com.itechart.project

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import com.itechart.project.configuration.AuthenticationSettings
import com.itechart.project.configuration.ConfigurationTypes.AppConfiguration
import com.itechart.project.context.AppContext
import com.itechart.project.resources.RedisResource
import dev.profunktor.redis4cats.RedisCommands
import io.chrisdavenport.log4cats.{Logger, SelfAwareStructuredLogger}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.config.parser
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    AuthenticationSettings
      .of[IO]
      .map(settings => RedisResource.make[IO](settings).map(redis => serverResource(redis.redis)).use(_ => IO.never))
  }

  private def serverResource[F[_]: ContextShift: ConcurrentEffect: Timer: Logger](
    redis: RedisCommands[F, String, String]
  ): Resource[F, Server[F]] = {
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
