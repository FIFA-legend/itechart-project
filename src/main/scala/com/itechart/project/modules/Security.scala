package com.itechart.project.modules

import cats.ApplicativeThrow
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.authentication.{Crypto, JwtExpire, Token}
import com.itechart.project.configuration.ConfigurationTypes._
import com.itechart.project.configuration.ConfigurationTypes.ClaimContent.jsonDecoder
import com.itechart.project.domain.user.{UserId, Username}
import com.itechart.project.dto.auth.{
  AuthClientUser,
  AuthCourierUser,
  AuthManagerUser,
  AuthUser,
  ClientJwtAuth,
  CourierJwtAuth,
  ManagerJwtAuth
}
import com.itechart.project.repository.UserRepository
import com.itechart.project.services.{Auth, UsersAuth}
import dev.profunktor.auth.jwt.{jwtDecode, JwtAuth, JwtToken}
import dev.profunktor.redis4cats.RedisCommands
import doobie.util.transactor.Transactor
import pdi.jwt.JwtAlgorithm
import eu.timepit.refined.auto._
import io.circe.parser.{decode => jsonDecode}

object Security {
  def of[F[_]: Sync](
    configuration: AuthenticationConfiguration,
    transactor:    Transactor[F],
    redis:         RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val managerJwtAuth: ManagerJwtAuth =
      ManagerJwtAuth(
        JwtAuth
          .hmac(
            configuration.managerJwtConfiguration.secretKey.value.secret,
            JwtAlgorithm.HS256
          )
      )

    val courierJwtAuth: CourierJwtAuth =
      CourierJwtAuth(
        JwtAuth
          .hmac(
            configuration.courierJwtConfiguration.secretKey.value.secret,
            JwtAlgorithm.HS256
          )
      )

    val clientJwtAuth: ClientJwtAuth =
      ClientJwtAuth(
        JwtAuth
          .hmac(
            configuration.tokenConfiguration.value.secret,
            JwtAlgorithm.HS256
          )
      )

    val managerToken = JwtToken(configuration.managerJwtConfiguration.managerToken.value.secret)
    val courierToken = JwtToken(configuration.courierJwtConfiguration.courierToken.value.secret)

    for {
      managerClaim <- jwtDecode[F](managerToken, managerJwtAuth.value)
      courierClaim <- jwtDecode[F](courierToken, courierJwtAuth.value)

      managerContent <- ApplicativeThrow[F].fromEither(jsonDecode[ClaimContent](managerClaim.content))
      courierContent <- ApplicativeThrow[F].fromEither(jsonDecode[ClaimContent](courierClaim.content))

      managerUser = AuthManagerUser(AuthUser(managerContent.uuid, Username("admin")))
      courierUser = AuthCourierUser(AuthUser(courierContent.uuid, Username("courier")))

      tokens <- JwtExpire
        .of[F]
        .map(Token.of[F](_, configuration.tokenConfiguration.value, configuration.tokenExpiration))
      crypto     <- Crypto.of[F](configuration.salt.value)
      users       = UserRepository.of[F](transactor)
      auth        = Auth.of[F](configuration.tokenExpiration, tokens, users, redis, crypto)
      managerAuth = UsersAuth.manager[F](managerToken, managerUser)
      courierAuth = UsersAuth.courier[F](courierToken, courierUser)
      clientAuth  = UsersAuth.client[F](redis)
    } yield new Security[F](
      auth,
      managerAuth,
      courierAuth,
      clientAuth,
      managerJwtAuth,
      courierJwtAuth,
      clientJwtAuth
    ) {}

  }
}

sealed abstract class Security[F[_]] private (
  val auth:           Auth[F],
  val managerAuth:    UsersAuth[F, AuthManagerUser],
  val courierAuth:    UsersAuth[F, AuthCourierUser],
  val clientAuth:     UsersAuth[F, AuthClientUser],
  val managerJwtAuth: ManagerJwtAuth,
  val courierJwtAuth: CourierJwtAuth,
  val clientJwtAuth:  ClientJwtAuth
)
