package com.itechart.project.functional_tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import com.itechart.project.dto.auth.LoginUser
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.functional_tests.Commons._
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class CategoryRoutesSpec extends AnyFreeSpec with Matchers {

  implicit val categoriesEntityDecoder: EntityDecoder[IO, List[CategoryDto]] = jsonOf[IO, List[CategoryDto]]
  implicit val categoryEntityDecoder:   EntityDecoder[IO, CategoryDto]       = jsonOf[IO, CategoryDto]
  implicit val categoryEntityEncoder:   EntityEncoder[IO, CategoryDto]       = jsonEncoderOf[IO, CategoryDto]

  implicit val longEntityDecoder:    EntityDecoder[IO, Long]    = jsonOf[IO, Long]
  implicit val booleanEntityDecoder: EntityDecoder[IO, Boolean] = jsonOf[IO, Boolean]

  implicit val userEntityEncoder: EntityEncoder[IO, LoginUser] = jsonEncoderOf[IO, LoginUser]

  val uri = uri"http://localhost:8080/categories"

  "Category routes tests" - {

    "Get all categories" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            categories <- getRequest[List[CategoryDto]](client, uri)
            result      = assert(categories.size == 3)
          } yield result
        )
        .unsafeRunSync()
    }

    "Get single category" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            actualCategory  <- getRequest[CategoryDto](client, uri / "1")
            expectedCategory = CategoryDto(1, "Laptops")
            result           = assert(actualCategory == expectedCategory)
          } yield result
        )
        .unsafeRunSync()
    }

    "Create, update and delete category" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            token <- login(client, LoginUser("ShopManager", "05082001Slonim"))

            createdCategory = CategoryDto(0, "Smart watch")
            responseCreatedCategory <- simplePostRequestWithAuth[CategoryDto, CategoryDto](
              client,
              createdCategory,
              uri,
              token
            )
            id                   = responseCreatedCategory.id
            createdCategoryById <- getRequest[CategoryDto](client, uri / id.toString)
            _                   <- assert(createdCategoryById == responseCreatedCategory).pure[IO]

            updatedCategory = CategoryDto(id, "Vacuum cleaner")
            responseUpdatedCategory <- simplePutRequestWithAuth[CategoryDto, CategoryDto](
              client,
              updatedCategory,
              uri,
              token
            )
            _                   <- assert(responseUpdatedCategory == updatedCategory).pure[IO]
            updatedCategoryById <- getRequest[CategoryDto](client, uri / id.toString)
            _                   <- assert(updatedCategoryById == updatedCategory).pure[IO]

            isCategoryDeleted <- simpleDeleteRequestWithAuth[Boolean](client, uri / id.toString, token)
            _                 <- assert(isCategoryDeleted).pure[IO]

            _ <- logout(client, token)
          } yield ()
        )
        .unsafeRunSync()
    }
  }

}
