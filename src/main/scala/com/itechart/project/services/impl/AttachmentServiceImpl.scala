package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.attachment.{AttachmentId, DatabaseAttachment}
import com.itechart.project.repository.AttachmentRepository
import com.itechart.project.services.AttachmentService
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError.AttachmentNotFound
import io.chrisdavenport.log4cats.Logger
import org.http4s.multipart.Multipart

import java.io.File

class AttachmentServiceImpl[F[_]: Sync: Logger](attachmentRepository: AttachmentRepository[F])
  extends AttachmentService[F] {
  private val path = "src/main/resources/attachments"

  override def findFileById(id: Long): F[Either[AttachmentFileError, File]] = {
    val result: EitherT[F, AttachmentFileError, File] = for {
      attachment <- EitherT.fromOptionF[F, AttachmentFileError, DatabaseAttachment](
        attachmentRepository.findById(AttachmentId(id)),
        AttachmentNotFound(id)
      )
      file <- EitherT.liftF[F, AttachmentFileError, File](new File(path + File.separator + attachment.link).pure[F])
    } yield file

    result.value
  }

  override def createFile(multipart: Multipart[F]): F[Either[AttachmentFileError, AttachmentId]] = ???

  override def deleteFile(id: Long): F[Either[AttachmentFileError, Boolean]] = ???
}
