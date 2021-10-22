package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.services.SupplierService
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError.{
  InvalidSupplierName,
  SupplierAlreadyExists,
  SupplierNotFound
}
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.util.Try

object SupplierRoutes {

  def routes[F[_]: Sync](supplierService: SupplierService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allSuppliers: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "suppliers" =>
      for {
        suppliers <- supplierService.findAllSuppliers
        response  <- Ok(suppliers)
      } yield response
    }

    def getSupplier: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "suppliers" / LongVar(id) =>
      val res = for {
        found <- supplierService.findById(id)
      } yield found

      marshalResponse(res)
    }

    def createSupplier: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "suppliers" =>
      val res = for {
        supplier <- req.as[SupplierDto]
        created  <- supplierService.createSupplier(supplier)
      } yield created

      marshalResponse(res)
    }

    def updateSupplier(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "suppliers" =>
      val res = for {
        supplier <- req.as[SupplierDto]
        updated  <- supplierService.updateSupplier(supplier)
      } yield updated

      marshalResponse(res)
    }

    def deleteSupplier(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "suppliers" / LongVar(id) =>
      val res = for {
        deleted <- supplierService.deleteSupplier(id)
      } yield deleted

      marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def supplierErrorToHttpResponse(error: SupplierValidationError): F[Response[F]] = {
      error match {
        case e: SupplierNotFound      => NotFound(e.message)
        case e: SupplierAlreadyExists => Conflict(e.message)
        case e @ InvalidSupplierName => BadRequest(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[SupplierValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => supplierErrorToHttpResponse(error)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage)
        }

    allSuppliers <+> getSupplier <+> updateSupplier() <+> createSupplier <+> deleteSupplier()
  }

}
