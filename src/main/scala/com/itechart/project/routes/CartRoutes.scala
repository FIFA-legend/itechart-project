package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.dto.cart.{CartDto, SingleCartDto}
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.routes.response.MarshalResponse.marshalResponse
import com.itechart.project.services.CartService
import com.itechart.project.services.error.CartErrors.CartValidationError
import com.itechart.project.services.error.CartErrors.CartValidationError._
import io.circe.generic.JsonCodec
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object CartRoutes {

  @JsonCodec final case class CartAndUser(cart: SingleCartDto, user: FullUserDto)

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](cartService: CartService[F]): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def currentCart: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ GET -> Root / "carts" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
      else {
        val res = for {
          user <- request.req.asJsonDecode[FullUserDto]
          cart <- cartService.findByUser(user)
        } yield cart

        marshalResponse[F, CartValidationError, CartDto](res, cartErrorToHttpResponse)
      }
    }

    def createCart: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ POST -> Root / "carts" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
      else {
        val res = for {
          cartAndUser <- request.req.asJsonDecode[CartAndUser]
          created     <- cartService.createCart(cartAndUser.cart, cartAndUser.user)
        } yield created

        marshalResponse[F, CartValidationError, SingleCartDto](res, cartErrorToHttpResponse)
      }
    }

    def updateCart(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ PUT -> Root / "carts" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
      else {
        val res = for {
          cartAndUser <- request.req.asJsonDecode[CartAndUser]
          updated     <- cartService.updateCart(cartAndUser.cart, cartAndUser.user)
        } yield updated

        marshalResponse[F, CartValidationError, SingleCartDto](res, cartErrorToHttpResponse)
      }
    }

    def deleteCart(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case DELETE -> Root / "carts" / LongVar(id) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          val res = for {
            deleted <- cartService.deleteCart(id)
          } yield deleted

          marshalResponse[F, CartValidationError, Boolean](res, cartErrorToHttpResponse)
        }
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

    currentCart <+> updateCart() <+> createCart <+> deleteCart()
  }

}
