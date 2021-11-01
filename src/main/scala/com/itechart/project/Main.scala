package com.itechart.project

import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.itechart.project.configuration.ConfigurationTypes.AppConfiguration
import com.itechart.project.context.AppContext
import io.circe.config.parser
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import dev.profunktor.redis4cats.log4cats._

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    serverResource[IO]
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  private def serverResource[F[_]: Async: Logger]: Resource[F, Server] = {
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
