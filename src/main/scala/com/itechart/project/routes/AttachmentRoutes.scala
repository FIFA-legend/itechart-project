package com.itechart.project.routes

import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import com.itechart.project.services.AttachmentService
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError.{
  AttachmentNotFound,
  InvalidItemAttachment
}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.{EntityEncoder, HttpRoutes, Response, StaticFile}

import scala.util.Try

object AttachmentRoutes {

  def routes[F[_]: Sync: ContextShift](attachmentService: AttachmentService[F], blocker: Blocker): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def getAttachment: HttpRoutes[F] = HttpRoutes.of[F] { case request @ GET -> Root / "attachments" / LongVar(id) =>
      for {
        file <- attachmentService.findFileById(id)
        response <- file match {
          case Right(value) => StaticFile.fromFile(value, blocker, Some(request)).getOrElseF(NotFound())
          case Left(value)  => NotFound(value.message)
        }
      } yield response
    }

    def deleteCategory(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "attachments" / LongVar(id) =>
      val res = for {
        deleted <- attachmentService.deleteFile(id)
      } yield deleted

      marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def attachmentErrorToHttpResponse(error: AttachmentFileError): F[Response[F]] = {
      error match {
        case e: AttachmentNotFound    => NotFound(e.message)
        case e: InvalidItemAttachment => Conflict(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[AttachmentFileError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => attachmentErrorToHttpResponse(error)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage)
        }

    getAttachment <+> deleteCategory()
  }

}
