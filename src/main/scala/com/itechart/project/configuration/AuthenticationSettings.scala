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

  /*def of[F[_]: Applicative]: F[AuthenticationConfiguration] = {
    AuthenticationConfiguration(
      ManagerJwtConfiguration(
        Secret(JwtSecretKeyConfiguration("a")),
        Secret(JwtClaimConfiguration("")),
        Secret(ManagerUserTokenConfiguration(""))
      ),
      CourierJwtConfiguration(
        Secret(JwtSecretKeyConfiguration("")),
        Secret(JwtClaimConfiguration("")),
        Secret(CourierUserTokenConfiguration(""))
      ),
      Secret(JwtAccessTokenKeyConfiguration("")),
      Secret(PasswordSalt("")),
      TokenExpiration(30.minutes),
      RedisConfiguration(RedisURI("redis://localhost"))
    ).pure[F]
  }*/

}
