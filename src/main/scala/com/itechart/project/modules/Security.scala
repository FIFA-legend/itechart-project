package com.itechart.project.modules

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.authentication.{Crypto, JwtExpire, Token}
import com.itechart.project.configuration.ConfigurationTypes._
import com.itechart.project.dto.auth.{LoggedInUser, UserJwtAuth}
import com.itechart.project.repository.UserRepository
import com.itechart.project.services.{Auth, UsersAuth}
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import eu.timepit.refined.auto._
import pdi.jwt.JwtAlgorithm

object Security {
  def of[F[_]: Sync](
    configuration: AuthenticationConfiguration,
    transactor:    Transactor[F],
    redis:         RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val userJwtAuth: UserJwtAuth =
      UserJwtAuth(
        JwtAuth
          .hmac(
            configuration.tokenConfiguration.value.secret,
            JwtAlgorithm.HS256
          )
      )

    for {
      tokens <- JwtExpire
        .of[F]
        .map(Token.of[F](_, configuration.tokenConfiguration.value, configuration.tokenExpiration))
      crypto  <- Crypto.of[F](configuration.salt.value)
      users    = UserRepository.of[F](transactor)
      auth     = Auth.of[F](configuration.tokenExpiration, tokens, users, redis, crypto)
      userAuth = UsersAuth.user[F](redis)
    } yield new Security[F](
      auth,
      userAuth,
      userJwtAuth
    ) {}

  }
}

sealed abstract class Security[F[_]] private (
  val auth:        Auth[F],
  val userAuth:    UsersAuth[F, LoggedInUser],
  val userJwtAuth: UserJwtAuth
)
