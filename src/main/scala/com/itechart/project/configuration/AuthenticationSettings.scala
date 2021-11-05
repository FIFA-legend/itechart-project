package com.itechart.project.configuration

import cats.implicits._
import cats.{Applicative, Show}
import ciris.Secret
import com.itechart.project.configuration.ConfigurationTypes._
import eu.timepit.refined.auto._

import scala.concurrent.duration.DurationInt

object AuthenticationSettings {

  implicit val accessTokenShow: Show[JwtAccessTokenKeyConfiguration] = Show.show(_.secret.value)
  implicit val saltShow:        Show[PasswordSalt]                   = Show.show(_.secret.value)

  def of[F[_]: Applicative]: F[AuthenticationConfiguration] = {
    AuthenticationConfiguration(
      Secret(JwtAccessTokenKeyConfiguration("5h0pp1ng_k4rt")),
      Secret(PasswordSalt("06!grsnxXG0d*Pj496p6fuA*o")),
      TokenExpiration(30.minutes),
      RedisConfiguration(RedisURI("redis://localhost"))
    ).pure[F]
  }

}
