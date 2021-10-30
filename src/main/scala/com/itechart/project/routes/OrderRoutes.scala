package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.order.OrderDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.services.OrderService
import com.itechart.project.services.error.OrderErrors.OrderValidationError
import com.itechart.project.services.error.OrderErrors.OrderValidationError.{
  InvalidOrderAddress,
  InvalidOrderCart,
  InvalidOrderStatus,
  InvalidOrderUser,
  OrderCartIsPartOfAnotherOrder,
  OrderNotFound
}
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.JsonCodec
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.util.Try

object OrderRoutes {

  def routes[F[_]: Sync: Logger](orderService: OrderService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allOrders: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "orders" =>
      for {
        orders   <- orderService.findAllOrders
        response <- Ok(orders)
      } yield response
    }

    def allOrdersByUser: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "orders" / "user" / LongVar(userId) =>
      val res = for {
        found <- orderService.findAllByUser(userId)
      } yield found

      marshalResponse(res)
    }

    def getOrder: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "orders" / LongVar(id) =>
      val res = for {
        found <- orderService.findById(id)
      } yield found

      marshalResponse(res)
    }

    def createOrder: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "orders" =>
      @JsonCodec final case class OrderAndUser(order: OrderDto, user: FullUserDto)

      val res = for {
        orderAndUser <- req.as[OrderAndUser]
        created      <- orderService.createOrder(orderAndUser.order, orderAndUser.user)
      } yield created

      marshalResponse(res)
    }

    def updateStatusToAssigned(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "orders" / LongVar(id) / "assigned" =>
        val res = for {
          updated <- orderService.updateOrderToAssigned(id)
        } yield updated

        marshalResponse(res)
    }

    def updateStatusToDelivered(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "orders" / LongVar(id) / "delivered" =>
        val res = for {
          updated <- orderService.updateOrderToDelivered(id)
        } yield updated

        marshalResponse(res)
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
          case Left(error) => orderErrorToHttpResponse(error) <* Logger[F].info("ERROR: " + error.message)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
        }

    allOrders <+> allOrdersByUser <+> getOrder <+> createOrder <+> updateStatusToAssigned() <+> updateStatusToDelivered()
  }

}
