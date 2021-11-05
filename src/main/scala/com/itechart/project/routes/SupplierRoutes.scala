package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.routes.response.MarshalResponse.marshalResponse
import com.itechart.project.services.SupplierService
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object SupplierRoutes {

  def routes[F[_]: Sync: Logger: JsonDecoder](supplierService: SupplierService[F]): HttpRoutes[F] = {
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

      marshalResponse[F, SupplierValidationError, SupplierDto](res, supplierErrorToHttpResponse)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    allSuppliers <+> getSupplier
  }

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](
    supplierService: SupplierService[F]
  ): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def createSupplier: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case request @ POST -> Root / "suppliers" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            supplier <- request.req.asJsonDecode[SupplierDto]
            created  <- supplierService.createSupplier(supplier)
          } yield created

          marshalResponse[F, SupplierValidationError, SupplierDto](res, supplierErrorToHttpResponse)
        }
    }

    def updateSupplier(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case request @ PUT -> Root / "suppliers" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            supplier <- request.req.asJsonDecode[SupplierDto]
            updated  <- supplierService.updateSupplier(supplier)
          } yield updated

          marshalResponse[F, SupplierValidationError, SupplierDto](res, supplierErrorToHttpResponse)
        }
    }

    def deleteSupplier(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case DELETE -> Root / "suppliers" / LongVar(id) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            deleted <- supplierService.deleteSupplier(id)
          } yield deleted

          marshalResponse[F, SupplierValidationError, Boolean](res, supplierErrorToHttpResponse)
        }
    }

    updateSupplier() <+> createSupplier <+> deleteSupplier()
  }

  private def supplierErrorToHttpResponse[F[_]: Sync: Logger](error: SupplierValidationError): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    error match {
      case e: SupplierNotFound    => NotFound(e.message)
      case e: SupplierInUse       => Conflict(e.message)
      case e: SupplierIsConnected => Conflict(e.message)
      case e @ InvalidSupplierName => BadRequest(e.message)
      case e: UnsupportedSupplierError => BadRequest(e.message)

      case e => BadRequest(e.message)
    }
  }

}
