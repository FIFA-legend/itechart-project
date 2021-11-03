package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.dto.order.OrderDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.services.OrderService
import com.itechart.project.services.error.OrderErrors.OrderValidationError
import com.itechart.project.services.error.OrderErrors.OrderValidationError._
import io.circe.generic.JsonCodec
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityEncoder, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object OrderRoutes {

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](orderService: OrderService[F]): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allOrders: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case GET -> Root / "orders" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager, Role.Courier))) Forbidden()
      else {
        for {
          orders   <- orderService.findAllOrders
          response <- Ok(orders)
        } yield response
      }
    }

    def allOrdersByUser: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case GET -> Root / "orders" / "user" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          val res = for {
            found <- orderService.findAllByUser(user.value.longId)
          } yield found

          marshalResponse(res)
        }
    }

    def getOrder: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case GET -> Root / "orders" / LongVar(id) as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
      else {
        val res = for {
          found <- orderService.findById(id)
        } yield found

        marshalResponse(res)
      }
    }

    def createOrder: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ POST -> Root / "orders" as user =>
      @JsonCodec final case class OrderAndUser(order: OrderDto, user: FullUserDto)

      if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
      else {
        val res = for {
          orderAndUser <- request.req.asJsonDecode[OrderAndUser]
          created      <- orderService.createOrder(orderAndUser.order, orderAndUser.user)
        } yield created

        marshalResponse(res)
      }
    }

    def updateStatusToAssigned(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case PUT -> Root / "orders" / LongVar(id) / "assigned" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Courier))) Forbidden()
        else {
          val res = for {
            updated <- orderService.updateOrderToAssigned(id)
          } yield updated

          marshalResponse(res)
        }
    }

    def updateStatusToDelivered(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case PUT -> Root / "orders" / LongVar(id) / "delivered" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Courier))) Forbidden()
        else {
          val res = for {
            updated <- orderService.updateOrderToDelivered(id)
          } yield updated

          marshalResponse(res)
        }
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def orderErrorToHttpResponse(error: OrderValidationError): F[Response[F]] = {
      error match {
        case e: OrderNotFound => NotFound(e.message)
        case e @ InvalidOrderAddress           => BadRequest(e.message)
        case e @ InvalidOrderStatus            => BadRequest(e.message)
        case e @ InvalidOrderCart              => BadRequest(e.message)
        case e @ OrderCartIsPartOfAnotherOrder => BadRequest(e.message)
        case e: InvalidOrderUser => BadRequest(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[OrderValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => orderErrorToHttpResponse(error) <* Logger[F].warn(error.message)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
        }

    allOrders <+> allOrdersByUser <+> getOrder <+> createOrder <+> updateStatusToAssigned() <+> updateStatusToDelivered()
  }

}
