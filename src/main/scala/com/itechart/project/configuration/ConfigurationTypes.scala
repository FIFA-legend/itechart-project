package com.itechart.project.configuration

import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.JsonCodec

import scala.concurrent.duration.FiniteDuration

object ConfigurationTypes {

  @JsonCodec
  final case class AppConfiguration(
    server: ServerConfiguration,
    db:     DatabaseConfiguration
  )

  @JsonCodec
  final case class DatabaseConfiguration(
    provider:          String,
    driver:            String,
    url:               String,
    user:              String,
    password:          String,
    migrationLocation: String
  )

  @JsonCodec
  final case class ServerConfiguration(
    host: String,
    port: Int
  )

  final case class ManagerUserTokenConfiguration(secret: NonEmptyString)

  final case class JwtSecretKeyConfiguration(secret: NonEmptyString)

  final case class JwtAccessTokenKeyConfiguration(secret: NonEmptyString)

  final case class JwtClaimConfiguration(secret: NonEmptyString)

  final case class PasswordSalt(secret: NonEmptyString)

  final case class TokenExpiration(value: FiniteDuration)

  final case class ManagerJwtConfiguration(
    secretKey:    Secret[JwtSecretKeyConfiguration],
    claimString:  Secret[JwtClaimConfiguration],
    managerToken: Secret[ManagerUserTokenConfiguration]
  )

}
