package com.itechart.project.services

import cats.effect.MonadThrow
import cats._
import cats.syntax.all._
import com.itechart.project.authentication.{Crypto, Token}
import com.itechart.project.configuration.ConfigurationTypes.TokenExpiration
import com.itechart.project.domain.user.{Password, Username}
import com.itechart.project.dto.auth.{AuthClientUser, AuthCourierUser, AuthManagerUser, AuthUser}
import com.itechart.project.repository.UserRepository
import com.itechart.project.services.error.AuthErrors.UserAuthenticationError.{InvalidPassword, UserNotFound}
import com.itechart.project.util.ModelMapper.userDomainToDto
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import pdi.jwt.JwtClaim
import io.circe.parser.decode
import io.circe.syntax._

import scala.tools.nsc.tasty.SafeEq

trait UsersAuth[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

object UsersAuth {
  def manager[F[_]: Applicative](
    managerToken: JwtToken,
    managerUser:  AuthManagerUser
  ): UsersAuth[F, AuthManagerUser] = new UsersAuth[F, AuthManagerUser] {
    override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AuthManagerUser]] = {
      (token === managerToken)
        .guard[Option]
        .as(managerUser)
        .pure[F]
    }
  }

  def courier[F[_]: Applicative](
    courierToken: JwtToken,
    courierUser:  AuthCourierUser
  ): UsersAuth[F, AuthCourierUser] = new UsersAuth[F, AuthCourierUser] {
    override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AuthCourierUser]] = {
      (token === courierToken)
        .guard[Option]
        .as(courierUser)
        .pure[F]
    }
  }

  def client[F[_]: Functor](
    redis: RedisCommands[F, String, String]
  ): UsersAuth[F, AuthClientUser] = new UsersAuth[F, AuthClientUser] {
    override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AuthClientUser]] = {
      redis
        .get(token.value)
        .map {
          _.flatMap { user =>
            decode[AuthUser](user).toOption.map(AuthClientUser.apply)
          }
        }
    }
  }
}

trait Auth[F[_]] {
  //def register(username: Username, password: Password, email: Email): F[JwtToken]
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

    /*override def register(username: Username, password: Password, email: Email): F[JwtToken] = {
      users.findByUsername(username).flatMap {
        case Some(_) => UsernameInUse(username).raiseError[F, JwtToken]
        case None =>
          for {
            id  <- users.create(authUserDtoToDomain())
            t   <- token.create
            user = AuthUser(id, username).asJson.noSpaces
            _   <- redis.setEx(t.value, user, expiration)
            _   <- redis.setEx(username.value.show, t.value, expiration)
          } yield t
      }
    }*/

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
                redis.setEx(t.value, userDomainToDto(user).asJson.noSpaces, expiration) *>
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
