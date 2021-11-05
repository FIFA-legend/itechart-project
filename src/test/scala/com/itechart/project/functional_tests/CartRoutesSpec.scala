package com.itechart.project.functional_tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoginUser
import com.itechart.project.dto.cart.{CartDto, SimpleItemDto, SingleCartDto}
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.functional_tests.Commons._
import com.itechart.project.routes.CartRoutes.CartAndUser
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.io.{BadRequest, Forbidden}
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class CartRoutesSpec extends AnyFreeSpec with Matchers {

  implicit val cartEntityDecoder: EntityDecoder[IO, CartDto] = jsonOf[IO, CartDto]

  implicit val singleCartEntityDecoder: EntityDecoder[IO, SingleCartDto] = jsonOf[IO, SingleCartDto]
  implicit val singleCartEntityEncoder: EntityEncoder[IO, SingleCartDto] = jsonEncoderOf[IO, SingleCartDto]

  implicit val fullUserEntityEncoder: EntityEncoder[IO, FullUserDto] = jsonEncoderOf[IO, FullUserDto]
  implicit val cartAndUserEncoder:    EntityEncoder[IO, CartAndUser] = jsonEncoderOf[IO, CartAndUser]

  implicit val longEntityDecoder:    EntityDecoder[IO, Long]    = jsonOf[IO, Long]
  implicit val booleanEntityDecoder: EntityDecoder[IO, Boolean] = jsonOf[IO, Boolean]

  implicit val userEntityEncoder: EntityEncoder[IO, LoginUser] = jsonEncoderOf[IO, LoginUser]

  val uri = uri"http://localhost:8080/carts"

  private val loginUser = LoginUser("KolodkoNikita", "05082001Slonim")

  private val clientUser = FullUserDto(
    3,
    "KolodkoNikita",
    "kolodkonikitos@gmail.com",
    Role.Client,
    List(CategoryDto(1, "Laptops"), CategoryDto(3, "Tablets")),
    List(SupplierDto(2, "Apple"))
  )

  "Cart routes tests" - {

    "Get cart for user" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            token <- login(client, loginUser)

            expectedCart = CartDto(List())
            actualCart  <- getRequestWithAuthAndBody[CartDto, FullUserDto](client, uri, clientUser, token)
            result       = assert(expectedCart == actualCart)

            _ <- logout(client, token)
          } yield result
        )
        .unsafeRunSync()
    }

    "Put item to cart, update amount and remove from cart" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            token <- login(client, loginUser)

            createdCartItem = SingleCartDto(
              0,
              1,
              SimpleItemDto(1, "ASUS TUF GAMING FX504", "Awesome gaming laptop", 1400),
              None
            )
            responseCreatedCartItem <- simplePostRequestWithAuth[SingleCartDto, CartAndUser](
              client,
              CartAndUser(createdCartItem, clientUser),
              uri,
              token
            )
            id = responseCreatedCartItem.id
            _ <- assert(responseCreatedCartItem == createdCartItem.copy(id = id)).pure[IO]

            updatedCartItem = createdCartItem.copy(id = id, quantity = 3)
            responseUpdatedCartItem <- simplePutRequestWithAuth[SingleCartDto, CartAndUser](
              client,
              CartAndUser(updatedCartItem, clientUser),
              uri,
              token
            )
            _ <- assert(responseUpdatedCartItem == updatedCartItem).pure[IO]

            isCartItemDeleted <- simpleDeleteRequestWithAuth[Boolean](client, uri / id.toString, token)
            _                 <- assert(isCartItemDeleted).pure[IO]

            _ <- logout(client, token)
          } yield ()
        )
        .unsafeRunSync()
    }

    "Invalid access to secured uri" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val cartItem =
            SingleCartDto(0, 1, SimpleItemDto(1, "ASUS TUF GAMING FX504", "Awesome gaming laptop", 1400), None)
          for {
            response <- postRequest(client, uri, CartAndUser(cartItem, clientUser))
            result    = checkResponseWithoutBody[SingleCartDto](response, Forbidden)
          } yield result
        }
        .unsafeRunSync()
    }

    "Invalid cart item input" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val notAvailableCartItem =
            SingleCartDto(0, 1, SimpleItemDto(3, "Iphone 12", "Apple phone on sale now", 550), None)
          val availableCartItem =
            SingleCartDto(0, 1000, SimpleItemDto(1, "ASUS TUF GAMING FX504", "Awesome gaming laptop", 1400), None)
          for {
            token <- login(client, loginUser)

            notAvailableResp <- postRequestWithAuth(client, uri, CartAndUser(notAvailableCartItem, clientUser), token)
            _                <- checkResponse(notAvailableResp, BadRequest, Some("\"The item with id `3` isn't available\"")).pure[IO]

            availableResp <- postRequestWithAuth(client, uri, CartAndUser(availableCartItem, clientUser), token)
            _             <- checkResponse(availableResp, BadRequest, Some("\"The quantity of item in stock is less\"")).pure[IO]

            _ <- logout(client, token)
          } yield ()
        }
        .unsafeRunSync()
    }
  }

}
