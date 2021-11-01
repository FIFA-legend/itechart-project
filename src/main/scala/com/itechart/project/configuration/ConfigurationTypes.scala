package com.itechart.project.configuration

import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.generic.JsonCodec

import scala.concurrent.duration.FiniteDuration

object ConfigurationTypes {

  @JsonCodec
  final case class AppConfiguration(
    server: ServerConfiguration,
    db:     DatabaseConfiguration,
    mail:   MailConfiguration
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

  @JsonCodec
  final case class MailConfiguration(
    host:     String,
    port:     Int,
    sender:   String,
    password: String
  )

  final case class AuthenticationConfiguration(
    managerJwtConfiguration: ManagerJwtConfiguration,
    courierJwtConfiguration: CourierJwtConfiguration,
    tokenConfiguration:      Secret[JwtAccessTokenKeyConfiguration],
    salt:                    Secret[PasswordSalt],
    tokenExpiration:         TokenExpiration,
    redisConfiguration:      RedisConfiguration
  )

  final case class ManagerJwtConfiguration(
    secretKey:    Secret[JwtSecretKeyConfiguration],
    claimString:  Secret[JwtClaimConfiguration],
    managerToken: Secret[ManagerUserTokenConfiguration]
  )

  final case class CourierJwtConfiguration(
    secretKey:    Secret[JwtSecretKeyConfiguration],
    claimString:  Secret[JwtClaimConfiguration],
    courierToken: Secret[CourierUserTokenConfiguration]
  )

  final case class RedisConfiguration(uri: RedisURI)

  final case class RedisURI(value: NonEmptyString)

  final case class ManagerUserTokenConfiguration(secret: NonEmptyString)

  final case class CourierUserTokenConfiguration(secret: NonEmptyString)

  final case class JwtSecretKeyConfiguration(secret: NonEmptyString)

  final case class JwtAccessTokenKeyConfiguration(secret: NonEmptyString)

  final case class JwtClaimConfiguration(secret: NonEmptyString)

  final case class PasswordSalt(secret: NonEmptyString)

  final case class TokenExpiration(value: FiniteDuration)

  final case class ClaimContent(id: Long)

  object ClaimContent {
    implicit val jsonDecoder: Decoder[ClaimContent] =
      Decoder.forProduct1("id")(ClaimContent.apply)
  }

}
