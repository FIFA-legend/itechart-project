package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.services.{AttachmentService, CategoryService}
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError.{
  AttachmentNotFound,
  InvalidItemAttachment
}
import fs2.io.file.Files
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.{AuthedRoutes, EntityEncoder, HttpRoutes, Response, StaticFile}
import org.typelevel.log4cats.Logger

import scala.util.Try

object AttachmentRoutes {

  def routes[F[_]: Sync: Logger: Files](
    attachmentService: AttachmentService[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def getAttachment: HttpRoutes[F] = HttpRoutes.of[F] { case request @ GET -> Root / "attachments" / LongVar(id) =>
      for {
        file <- attachmentService.findFileById(id)
        response <- file match {
          case Right(value) => StaticFile.fromFile(value, Some(request)).getOrElseF(NotFound())
          case Left(value)  => NotFound(value.message)
        }
      } yield response
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    getAttachment
  }

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](
    attachmentService: AttachmentService[F]
  ): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def deleteCategory(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case DELETE -> Root / "attachments" / LongVar(id) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            deleted <- attachmentService.deleteFile(id)
          } yield deleted

          marshalResponse(res)
        }
    }

    deleteCategory()
  }

  def attachmentErrorToHttpResponse[F[_]: Sync: Logger](error: AttachmentFileError): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    error match {
      case e: AttachmentNotFound    => NotFound(e.message)
      case e: InvalidItemAttachment => Conflict(e.message)

      case e => BadRequest(e.message)
    }
  }

  def marshalResponse[F[_]: Sync: Logger, T](
    result: F[Either[AttachmentFileError, T]]
  )(
    implicit E: EntityEncoder[F, T]
  ): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    result
      .flatMap {
        case Left(error) => attachmentErrorToHttpResponse(error) <* Logger[F].info("ERROR: " + error.message)
        case Right(dto)  => Ok(dto)
      }
      .handleErrorWith { ex =>
        InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
      }
  }

}
