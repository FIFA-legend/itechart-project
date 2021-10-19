package com.itechart.project.configuration

import cats.implicits._
import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway

object DatabaseSettings {

  def transactor[F[_]: ContextShift: Async](
    configuration: DatabaseConfiguration
  ): Resource[F, Transactor[F]] = for {
    pool    <- ExecutionContexts.fixedThreadPool[F](10)
    blocker <- Blocker[F]
    transactor <- HikariTransactor.newHikariTransactor[F](
      driverClassName = configuration.driver,
      url             = configuration.url,
      user            = configuration.user,
      pass            = configuration.password,
      connectEC       = pool,
      blocker         = blocker
    )
  } yield transactor

  class FlywayMigrator[F[_]: Sync](configuration: DatabaseConfiguration) {
    def migrate(): F[Int] =
      for {
        conf <- migrationConfiguration(configuration)
        res  <- Sync[F].delay(conf.migrate())
      } yield res

    private def migrationConfiguration(configuration: DatabaseConfiguration): F[Flyway] = {
      Sync[F].delay(
        Flyway
          .configure()
          .dataSource(configuration.url, configuration.user, configuration.password)
          .locations(s"${configuration.migrationLocation}/${configuration.provider}")
          .load()
      )
    }
  }

  def migrator[F[_]: Sync](configuration: DatabaseConfiguration): F[FlywayMigrator[F]] =
    new FlywayMigrator[F](configuration).pure[F]

}