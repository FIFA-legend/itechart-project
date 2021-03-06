package com.itechart.project.configuration

import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.generic.JsonCodec

import java.util.UUID
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
    tokenConfiguration: Secret[JwtAccessTokenKeyConfiguration],
    salt:               Secret[PasswordSalt],
    tokenExpiration:    TokenExpiration,
    redisConfiguration: RedisConfiguration
  )

  final case class RedisConfiguration(uri: RedisURI)

  final case class RedisURI(value: NonEmptyString)

  final case class JwtAccessTokenKeyConfiguration(secret: NonEmptyString)

  final case class PasswordSalt(secret: NonEmptyString)

  final case class TokenExpiration(value: FiniteDuration)

  @JsonCodec
  final case class ClaimContent(uuid: UUID)

  object ClaimContent {
    implicit val jsonDecoder: Decoder[ClaimContent] =
      Decoder.forProduct1("uuid")(ClaimContent.apply)
  }

}
