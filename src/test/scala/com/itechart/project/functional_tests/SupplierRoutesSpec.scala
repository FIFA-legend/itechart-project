package com.itechart.project.functional_tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import com.itechart.project.dto.auth.LoginUser
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.functional_tests.Commons._
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class SupplierRoutesSpec extends AnyFreeSpec with Matchers {

  implicit val suppliersEntityDecoder: EntityDecoder[IO, List[SupplierDto]] = jsonOf[IO, List[SupplierDto]]
  implicit val supplierEntityDecoder:  EntityDecoder[IO, SupplierDto]       = jsonOf[IO, SupplierDto]
  implicit val supplierEntityEncoder:  EntityEncoder[IO, SupplierDto]       = jsonEncoderOf[IO, SupplierDto]

  implicit val longEntityDecoder:    EntityDecoder[IO, Long]    = jsonOf[IO, Long]
  implicit val booleanEntityDecoder: EntityDecoder[IO, Boolean] = jsonOf[IO, Boolean]

  implicit val userEntityEncoder: EntityEncoder[IO, LoginUser] = jsonEncoderOf[IO, LoginUser]

  val uri = uri"http://localhost:8080/suppliers"

  "Supplier routes tests" - {

    "Get all suppliers" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            suppliers <- getRequest[List[SupplierDto]](client, uri)
            result     = assert(suppliers.size == 2)
          } yield result
        )
        .unsafeRunSync()
    }

    "Get single supplier" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            actualSupplier  <- getRequest[SupplierDto](client, uri / "1")
            expectedSupplier = SupplierDto(1, "ASUS")
            result           = assert(actualSupplier == expectedSupplier)
          } yield result
        )
        .unsafeRunSync()
    }

    "Create, update and delete supplier" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            token <- login(client, LoginUser("ShopManager", "05082001Slonim"))

            createdSupplier = SupplierDto(0, "Samsung")
            responseCreatedSupplier <- simplePostRequestWithAuth[SupplierDto, SupplierDto](
              client,
              createdSupplier,
              uri,
              token
            )
            id                   = responseCreatedSupplier.id
            createdSupplierById <- getRequest[SupplierDto](client, uri / id.toString)
            _                   <- assert(createdSupplierById == responseCreatedSupplier).pure[IO]

            updatedSupplier = SupplierDto(id, "Xiaomi")
            responseSupplierCategory <- simplePutRequestWithAuth[SupplierDto, SupplierDto](
              client,
              updatedSupplier,
              uri,
              token
            )
            _                   <- assert(responseSupplierCategory == updatedSupplier).pure[IO]
            updatedSupplierById <- getRequest[SupplierDto](client, uri / id.toString)
            _                   <- assert(updatedSupplierById == updatedSupplier).pure[IO]

            isSupplierDeleted <- simpleDeleteRequestWithAuth[Boolean](client, uri / id.toString, token)
            _                 <- assert(isSupplierDeleted).pure[IO]

            _ <- logout(client, token)
          } yield ()
        )
        .unsafeRunSync()
    }
  }

}
