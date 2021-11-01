package com.itechart.project.configuration

import cats.{Applicative, Show}
import ciris.Secret
import cats.implicits._
import com.itechart.project.configuration.ConfigurationTypes.{
  AuthenticationConfiguration,
  CourierJwtConfiguration,
  CourierUserTokenConfiguration,
  JwtAccessTokenKeyConfiguration,
  JwtClaimConfiguration,
  JwtSecretKeyConfiguration,
  ManagerJwtConfiguration,
  ManagerUserTokenConfiguration,
  PasswordSalt,
  RedisConfiguration,
  RedisURI,
  TokenExpiration
}
import eu.timepit.refined.auto._

import scala.concurrent.duration.DurationInt

object AuthenticationSettings {

  implicit val secretKeyShow:    Show[JwtSecretKeyConfiguration]      = Show.show(_.secret.value)
  implicit val jwtClaimShow:     Show[JwtClaimConfiguration]          = Show.show(_.secret.value)
  implicit val managerTokenShow: Show[ManagerUserTokenConfiguration]  = Show.show(_.secret.value)
  implicit val courierTokenShow: Show[CourierUserTokenConfiguration]  = Show.show(_.secret.value)
  implicit val accessTokenShow:  Show[JwtAccessTokenKeyConfiguration] = Show.show(_.secret.value)
  implicit val saltShow:         Show[PasswordSalt]                   = Show.show(_.secret.value)

  def of[F[_]: Applicative]: F[AuthenticationConfiguration] = {
    AuthenticationConfiguration(
      ManagerJwtConfiguration(
        Secret(JwtSecretKeyConfiguration("-*5h0pp1ng_k4rt*-")),
        Secret(JwtClaimConfiguration("{\"uuid\": \"004b4457-71c3-4439-a1b2-03820263b59c\"}")),
        Secret(
          ManagerUserTokenConfiguration(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjogIjAwNGI0NDU3LTcxYzMtNDQzOS1hMWIyLTAzODIwMjYzYjU5YyJ9.L97BnPScSAKY-BLkYu8G_n8h1U4LDOURUen14O22hD4"
          )
        )
      ),
      CourierJwtConfiguration(
        Secret(JwtSecretKeyConfiguration("-*5h0pp1ng_k4rt*-")),
        Secret(JwtClaimConfiguration("{\"uuid\": \"05082001-71c3-4439-a1b2-03820263b59c\"}")),
        Secret(
          CourierUserTokenConfiguration(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1dWlkIjogIjAwNGI0NDU3LTcxYzMtNDQzOS1hMWIyLTAzODIwMjYzYjU5YyJ9.L97BnPScSAKY-BLkYu8G_n8h1U4LDOURUen14O22hD4"
          )
        )
      ),
      Secret(JwtAccessTokenKeyConfiguration("5h0pp1ng_k4rt")),
      Secret(PasswordSalt("06!grsnxXG0d*Pj496p6fuA*o")),
      TokenExpiration(30.minutes),
      RedisConfiguration(RedisURI("redis://localhost"))
    ).pure[F]
  }

}
