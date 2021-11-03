package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.dto.user.UserDto
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.services.UserService
import com.itechart.project.services.error.UserErrors.UserValidationError
import com.itechart.project.services.error.UserErrors.UserValidationError._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityEncoder, HttpRoutes, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object UserRoutes {

  def routes[F[_]: Sync: Logger: JsonDecoder](userService: UserService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def createUser: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
      val res = for {
        user    <- req.asJsonDecode[UserDto]
        created <- userService.createUser(user)
      } yield created

      marshalResponse(res)
    }

    createUser
  }

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](
    userService: UserService[F]
  ): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allUsers: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case GET -> Root / "users" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
      else {
        for {
          users    <- userService.findAllUsers
          response <- Ok(users)
        } yield response
      }
    }

    def getUser: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case GET -> Root / "users" / LongVar(id) as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager, Role.Client))) Forbidden()
      else {
        val res = for {
          found <- userService.findById(id)
        } yield found

        marshalResponse(res)
      }
    }

    def updateUser(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case request @ PUT -> Root / "users" / LongVar(id) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager, Role.Client))) Forbidden()
        else {
          val res = for {
            user    <- request.req.asJsonDecode[UserDto]
            updated <- userService.updateUser(id, user)
          } yield updated

          marshalResponse(res)
        }
    }

    def subscribeOnCategory(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case POST -> Root / "category" / LongVar(categoryId) / "subscribe" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          val res = for {
            updated <- userService.subscribeCategory(user.value.longId, categoryId)
          } yield updated

          marshalResponse(res)
        }
    }

    def unsubscribeFromCategory(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case POST -> Root / "category" / LongVar(categoryId) / "unsubscribe" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          val res = for {
            updated <- userService.unsubscribeCategory(user.value.longId, categoryId)
          } yield updated

          marshalResponse(res)
        }
    }

    def subscribeOnSupplier(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case POST -> Root / "supplier" / LongVar(supplierId) / "subscribe" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          val res = for {
            updated <- userService.subscribeSupplier(user.value.longId, supplierId)
          } yield updated

          marshalResponse(res)
        }
    }

    def unsubscribeFromSupplier(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case POST -> Root / "supplier" / LongVar(supplierId) / "unsubscribe" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          val res = for {
            updated <- userService.unsubscribeSupplier(user.value.longId, supplierId)
          } yield updated

          marshalResponse(res)
        }
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    allUsers <+> getUser <+> updateUser() <+> subscribeOnCategory() <+> unsubscribeFromCategory() <+>
      subscribeOnSupplier() <+> unsubscribeFromSupplier()
  }

  private def userErrorToHttpResponse[F[_]: Sync: Logger](error: UserValidationError): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    error match {
      case e: UserNotFound  => NotFound(e.message)
      case e: UsernameInUse => Conflict(e.message)
      case e: EmailInUse    => Conflict(e.message)
      case e @ InvalidUsernameSymbols => BadRequest(e.message)
      case e @ InvalidUsernameLength  => BadRequest(e.message)
      case e @ InvalidPassword        => BadRequest(e.message)
      case e @ InvalidEmail           => BadRequest(e.message)

      case e => BadRequest(e.message)
    }
  }

  private def marshalResponse[F[_]: Sync: Logger, T](
    result: F[Either[UserValidationError, T]]
  )(
    implicit E: EntityEncoder[F, T]
  ): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    result
      .flatMap {
        case Left(error) => userErrorToHttpResponse(error) <* Logger[F].warn(error.message)
        case Right(dto)  => Ok(dto)
      }
      .handleErrorWith { ex =>
        InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
      }
  }

}
