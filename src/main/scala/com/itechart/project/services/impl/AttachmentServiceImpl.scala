package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import com.itechart.project.domain.attachment.{AttachmentId, DatabaseAttachment}
import com.itechart.project.repository.AttachmentRepository
import com.itechart.project.services.AttachmentService
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError.AttachmentNotFound
import io.chrisdavenport.log4cats.Logger

import java.io.File
import java.nio.file.{Files, Paths}

class AttachmentServiceImpl[F[_]: Sync: Logger: ContextShift](
  attachmentRepository: AttachmentRepository[F]
) extends AttachmentService[F] {
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

  override def deleteFile(id: Long): F[Either[AttachmentFileError, Boolean]] = {
    val result: EitherT[F, AttachmentFileError, Boolean] = for {
      attachment    <- EitherT.fromOptionF(attachmentRepository.findById(AttachmentId(id)), AttachmentNotFound(id))
      isFileDeleted <- EitherT.liftF(Files.deleteIfExists(Paths.get(path + File.separator + attachment.link)).pure[F])
      attachmentDeleted <-
        if (!isFileDeleted) EitherT.liftF[F, AttachmentFileError, Int](0.pure[F])
        else EitherT.liftF[F, AttachmentFileError, Int](attachmentRepository.delete(attachment.id))
    } yield attachmentDeleted != 0

    result.value
  }
}
