package com.itechart.project.configuration

import cats.implicits._
import cats.effect.{Async, Resource, Sync}
import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway

import java.io.File

object DatabaseSettings {

  def transactor[F[_]: Async](
    configuration: DatabaseConfiguration
  ): Resource[F, Transactor[F]] = for {
    pool <- ExecutionContexts.fixedThreadPool[F](10)
    transactor <- HikariTransactor.newHikariTransactor[F](
      driverClassName = configuration.driver,
      url             = configuration.url,
      user            = configuration.user,
      pass            = configuration.password,
      connectEC       = pool
    )
  } yield transactor

  class FlywayMigrator[F[_]: Sync](configuration: DatabaseConfiguration) {
    def migrate(): F[Int] =
      for {
        config <- migrationConfiguration(configuration)
        res    <- Sync[F].delay(config.migrate())
      } yield res

    private def migrationConfiguration(configuration: DatabaseConfiguration): F[Flyway] = {
      Sync[F].delay(
        Flyway
          .configure()
          .dataSource(configuration.url, configuration.user, configuration.password)
          .locations(s"${configuration.migrationLocation}" + File.separator + s"${configuration.provider}")
          .load()
      )
    }
  }

  def migrator[F[_]: Sync](configuration: DatabaseConfiguration): F[FlywayMigrator[F]] =
    new FlywayMigrator[F](configuration).pure[F]

}
