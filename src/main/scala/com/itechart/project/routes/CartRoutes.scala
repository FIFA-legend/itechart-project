package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.cart.SingleCartDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.services.CartService
import com.itechart.project.services.error.CartErrors.CartValidationError
import com.itechart.project.services.error.CartErrors.CartValidationError.{
  CartIsPartOfOrder,
  CartItemIsNotAvailable,
  CartNotFound,
  CartQuantityIsOutOfBounds,
  InvalidCartItem,
  InvalidCartQuantity,
  InvalidCartUser,
  ItemIsAlreadyInCart
}
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.JsonCodec
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.util.Try

object CartRoutes {

  def routes[F[_]: Sync: Logger](cartService: CartService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    @JsonCodec final case class CartAndUser(cart: SingleCartDto, user: FullUserDto)

    def currentCart: HttpRoutes[F] = HttpRoutes.of[F] { case req @ GET -> Root / "carts" =>
      val res = for {
        user <- req.as[FullUserDto]
        cart <- cartService.findByUser(user)
      } yield cart

      marshalResponse(res)
    }

    def createCart: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "carts" =>
      val res = for {
        cartAndUser <- req.as[CartAndUser]
        created     <- cartService.createCart(cartAndUser.cart, cartAndUser.user)
      } yield created

      marshalResponse(res)
    }

    def updateCart(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "carts" =>
      val res = for {
        cartAndUser <- req.as[CartAndUser]
        updated     <- cartService.updateCart(cartAndUser.cart, cartAndUser.user)
      } yield updated

      marshalResponse(res)
    }

    def deleteCart(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "carts" / LongVar(id) =>
      val res = for {
        deleted <- cartService.deleteCart(id)
      } yield deleted

      marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def cartErrorToHttpResponse(error: CartValidationError): F[Response[F]] = {
      error match {
        case e: CartNotFound => NotFound(e.message)
        case e @ InvalidCartQuantity       => BadRequest(e.message)
        case e @ CartQuantityIsOutOfBounds => BadRequest(e.message)
        case e: CartItemIsNotAvailable => BadRequest(e.message)
        case e: InvalidCartUser        => BadRequest(e.message)
        case e: InvalidCartItem        => BadRequest(e.message)
        case e: ItemIsAlreadyInCart    => BadRequest(e.message)
        case e: CartIsPartOfOrder      => BadRequest(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[CartValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => cartErrorToHttpResponse(error) <* Logger[F].info("ERROR: " + error.message)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
        }

    currentCart <+> updateCart() <+> createCart <+> deleteCart()
  }

}
