package com.itechart.project.functional_tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import com.itechart.project.domain.item.AvailabilityStatus
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoginUser
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.item.{AttachmentIdDto, FilterItemDto, ItemDto}
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.functional_tests.Commons._
import com.itechart.project.routes.ItemRoutes.UserAndFilter
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class ItemRoutesSpec extends AnyFreeSpec with Matchers {

  implicit val itemsEntityDecoder: EntityDecoder[IO, List[ItemDto]] = jsonOf[IO, List[ItemDto]]
  implicit val itemEntityDecoder:  EntityDecoder[IO, ItemDto]       = jsonOf[IO, ItemDto]
  implicit val itemEntityEncoder:  EntityEncoder[IO, ItemDto]       = jsonEncoderOf[IO, ItemDto]

  implicit val itemFilterEncoder:     EntityEncoder[IO, FilterItemDto] = jsonEncoderOf[IO, FilterItemDto]
  implicit val fullUserEntityEncoder: EntityEncoder[IO, FullUserDto]   = jsonEncoderOf[IO, FullUserDto]
  implicit val userAndFilterEncoder:  EntityEncoder[IO, UserAndFilter] = jsonEncoderOf[IO, UserAndFilter]

  implicit val longEntityDecoder:    EntityDecoder[IO, Long]    = jsonOf[IO, Long]
  implicit val booleanEntityDecoder: EntityDecoder[IO, Boolean] = jsonOf[IO, Boolean]

  implicit val userEntityEncoder: EntityEncoder[IO, LoginUser] = jsonEncoderOf[IO, LoginUser]

  val uri = uri"http://localhost:8080/items"

  "Item routes tests" - {

    "Get all items" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            items <- getRequest[List[ItemDto]](client, uri)
            result = assert(items.size == 3)
          } yield result
        )
        .unsafeRunSync()
    }

    "Get all items by filter" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val filter = FilterItemDto(None, None, Some(600), Some(1500), List(), List())
          for {
            items <- getRequestWithBody[List[ItemDto], FilterItemDto](client, uri / "filter", filter)
            result = assert(items.size == 2)
          } yield result
        }
        .unsafeRunSync()
    }

    "Get all items by user" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val user = FullUserDto(
            3,
            "KolodkoNikita",
            "kolodkonikitos@gmail.com",
            Role.Client,
            List(CategoryDto(1, "Laptops"), CategoryDto(3, "Tablets")),
            List(SupplierDto(2, "Apple"))
          )
          for {
            token <- login(client, LoginUser("KolodkoNikita", "05082001Slonim"))

            items <- getRequestWithAuthAndBody[List[ItemDto], FullUserDto](
              client,
              uri / "available",
              user,
              token
            )
            result = assert(items.size == 3)

            _ <- logout(client, token)
          } yield result
        }
        .unsafeRunSync()
    }

    "Get all items by user and filter" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val filter = FilterItemDto(None, None, Some(600), Some(1500), List(), List())
          val user = FullUserDto(
            3,
            "KolodkoNikita",
            "kolodkonikitos@gmail.com",
            Role.Client,
            List(CategoryDto(1, "Laptops"), CategoryDto(3, "Tablets")),
            List(SupplierDto(2, "Apple"))
          )
          for {
            token <- login(client, LoginUser("KolodkoNikita", "05082001Slonim"))

            items <- getRequestWithAuthAndBody[List[ItemDto], UserAndFilter](
              client,
              uri / "available" / "filter",
              UserAndFilter(user, filter),
              token
            )
            result = assert(items.size == 2)

            _ <- logout(client, token)
          } yield result
        }
        .unsafeRunSync()
    }

    "Get single item" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            actualItem <- getRequest[ItemDto](client, uri / "1")
            expectedItem = ItemDto(
              1,
              "ASUS TUF GAMING FX504",
              "Awesome gaming laptop",
              50,
              1400,
              AvailabilityStatus.Available,
              SupplierDto(1, "ASUS"),
              List(CategoryDto(1, "Laptops")),
              List(AttachmentIdDto(1))
            )
            result = assert(actualItem == expectedItem)
          } yield result
        )
        .unsafeRunSync()
    }

    "Create, update and delete item" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            token <- login(client, LoginUser("ShopManager", "05082001Slonim"))

            createdItem = ItemDto(
              0,
              "ASUS TUF GAMING FX505",
              "Such as FX504 but better",
              50,
              1500,
              AvailabilityStatus.InProcessing,
              SupplierDto(1, "ASUS"),
              List(CategoryDto(3, "Tablets")),
              List()
            )
            responseCreatedItem <- simplePostRequestWithAuth[ItemDto, ItemDto](client, createdItem, uri, token)
            id                   = responseCreatedItem.id
            createdItemById     <- getRequest[ItemDto](client, uri / id.toString)
            _                   <- assert(createdItemById == responseCreatedItem).pure[IO]

            updatedItem = createdItem
              .copy(
                id          = id,
                description = "Such as FX504 but much better",
                amount      = 40,
                price       = 1550,
                categories  = List(CategoryDto(1, "Laptops"))
              )
            responseItemCategory <- simplePutRequestWithAuth[ItemDto, ItemDto](client, updatedItem, uri, token)
            _                    <- assert(responseItemCategory == updatedItem).pure[IO]
            updatedItemById      <- getRequest[ItemDto](client, uri / id.toString)
            _                    <- assert(updatedItemById == updatedItem).pure[IO]

            isItemDeleted <- simpleDeleteRequestWithAuth[Boolean](client, uri / id.toString, token)
            _             <- assert(isItemDeleted).pure[IO]

            _ <- logout(client, token)
          } yield ()
        )
        .unsafeRunSync()
    }
  }

}
