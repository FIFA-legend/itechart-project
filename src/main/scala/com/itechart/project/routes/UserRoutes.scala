package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.user.UserDto
import com.itechart.project.services.UserService
import com.itechart.project.services.error.UserErrors.UserValidationError
import com.itechart.project.services.error.UserErrors.UserValidationError._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object UserRoutes {

  def routes[F[_]: Sync: Logger: JsonDecoder](userService: UserService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allUsers: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "users" =>
      for {
        users    <- userService.findAllUsers
        response <- Ok(users)
      } yield response
    }

    def getUser: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "users" / LongVar(id) =>
      val res = for {
        found <- userService.findById(id)
      } yield found

      marshalResponse(res)
    }

    def createUser: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
      val res = for {
        user    <- req.asJsonDecode[UserDto]
        created <- userService.createUser(user)
      } yield created

      marshalResponse(res)
    }

    def updateUser(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "users" / LongVar(id) =>
      val res = for {
        user    <- req.asJsonDecode[UserDto]
        updated <- userService.updateUser(id, user)
      } yield updated

      marshalResponse(res)
    }

    def subscribeOnCategory(): HttpRoutes[F] = HttpRoutes.of[F] {
      case POST -> Root / "user" / LongVar(userId) / "category" / LongVar(categoryId) / "subscribe" =>
        val res = for {
          updated <- userService.subscribeCategory(userId, categoryId)
        } yield updated

        marshalResponse(res)
    }

    def unsubscribeFromCategory(): HttpRoutes[F] = HttpRoutes.of[F] {
      case POST -> Root / "user" / LongVar(userId) / "category" / LongVar(categoryId) / "unsubscribe" =>
        val res = for {
          updated <- userService.unsubscribeCategory(userId, categoryId)
        } yield updated

        marshalResponse(res)
    }

    def subscribeOnSupplier(): HttpRoutes[F] = HttpRoutes.of[F] {
      case POST -> Root / "user" / LongVar(userId) / "supplier" / LongVar(supplierId) / "subscribe" =>
        val res = for {
          updated <- userService.subscribeSupplier(userId, supplierId)
        } yield updated

        marshalResponse(res)
    }

    def unsubscribeFromSupplier(): HttpRoutes[F] = HttpRoutes.of[F] {
      case POST -> Root / "user" / LongVar(userId) / "supplier" / LongVar(supplierId) / "unsubscribe" =>
        val res = for {
          updated <- userService.unsubscribeSupplier(userId, supplierId)
        } yield updated

        marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def userErrorToHttpResponse(error: UserValidationError): F[Response[F]] = {
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

    def marshalResponse[T](
      result: F[Either[UserValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => userErrorToHttpResponse(error) <* Logger[F].info("ERROR: " + error.message)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
        }

    allUsers <+> getUser <+> createUser <+> updateUser() <+> subscribeOnCategory() <+> unsubscribeFromCategory() <+>
      subscribeOnSupplier() <+> unsubscribeFromSupplier()
  }

}
