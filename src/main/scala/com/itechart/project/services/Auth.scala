package com.itechart.project.services

import cats._
import cats.syntax.all._
import com.itechart.project.authentication.{Crypto, Token}
import com.itechart.project.configuration.ConfigurationTypes.TokenExpiration
import com.itechart.project.domain.user.{Password, Username}
import com.itechart.project.dto.auth.{AuthUser, LoggedInUser}
import com.itechart.project.repository.UserRepository
import com.itechart.project.services.error.AuthErrors.UserAuthenticationError.{InvalidPassword, UserNotFound}
import com.itechart.project.util.ModelMapper.userDomainToAuthUserDto
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import io.circe.syntax._
import pdi.jwt.JwtClaim

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

object UsersAuth {
  def user[F[_]: Functor](
    redis: RedisCommands[F, String, String]
  ): UsersAuth[F, LoggedInUser] = new UsersAuth[F, LoggedInUser] {
    override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[LoggedInUser]] = {
      redis
        .get(token.value)
        .map {
          _.flatMap { user =>
            decode[AuthUser](user).toOption.map(LoggedInUser.apply)
          }
        }
    }
  }
}

trait Auth[F[_]] {
  def login(username:   Username, password: Password): F[JwtToken]
  def logout(userToken: JwtToken, username: Username): F[Unit]
}

object Auth {
  def of[F[_]: MonadThrow](
    tokenExpiration: TokenExpiration,
    token:           Token[F],
    users:           UserRepository[F],
    redis:           RedisCommands[F, String, String],
    crypto:          Crypto
  ): Auth[F] = new Auth[F] {
    private val expiration = tokenExpiration.value

    override def login(username: Username, password: Password): F[JwtToken] = {
      users.findByUsername(username).flatMap {
        case None => UserNotFound(username).raiseError[F, JwtToken]
        case Some(user) if user.password.value != crypto.encrypt(password).value =>
          InvalidPassword(username).raiseError[F, JwtToken]
        case Some(user) =>
          redis.get(username.value.show).flatMap {
            case Some(t) => JwtToken(t).pure[F]
            case None =>
              token.create.flatTap { t =>
                redis.setEx(t.value, userDomainToAuthUserDto(user).asJson.noSpaces, expiration) *>
                  redis.setEx(username.value.show, t.value, expiration)
              }
          }
      }
    }

    override def logout(userToken: JwtToken, username: Username): F[Unit] = {
      redis.del(userToken.value.show) *> redis.del(username.value.show).void
    }
  }
}
