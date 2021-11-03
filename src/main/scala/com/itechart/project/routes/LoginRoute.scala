package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.{Password, Username}
import com.itechart.project.dto.auth.{AuthClientUser, LoginUser}
import com.itechart.project.services.Auth
import com.itechart.project.services.error.AuthErrors.UserAuthenticationError.{InvalidPassword, UserNotFound}
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto._
import org.http4s.circe.{jsonEncoderOf, toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, EntityEncoder, HttpRoutes}
import org.typelevel.log4cats.Logger

object LoginRoute {

  def routes[F[_]: Sync: Logger: JsonDecoder](
    auth:           Auth[F],
    authMiddleware: AuthMiddleware[F, AuthClientUser]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    implicit val jwtEntityEncoder: EntityEncoder[F, JwtToken] = jsonEncoderOf[F, JwtToken]

    def login: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
      req.asJsonDecode[LoginUser].flatMap { user =>
        auth
          .login(Username(user.username), Password(user.password))
          .flatMap(Ok(_))
          .recoverWith {
            case e: UserNotFound    => Forbidden(e.message)
            case e: InvalidPassword => Forbidden(e.message)
          }
      }
    }

    def logout: AuthedRoutes[AuthClientUser, F] = AuthedRoutes.of { case ar @ POST -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(auth.logout(_, user.value.username)) *> NoContent()
    }

    login <+> authMiddleware(logout)
  }

}
