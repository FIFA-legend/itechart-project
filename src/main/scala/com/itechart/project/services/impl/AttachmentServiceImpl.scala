package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.attachment.AttachmentId
import com.itechart.project.repository.AttachmentRepository
import com.itechart.project.services.AttachmentService
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError.AttachmentNotFound
import org.typelevel.log4cats.Logger

import java.io.File
import java.nio.file.{Files, Paths}

class AttachmentServiceImpl[F[_]: Sync: Logger](
  attachmentRepository: AttachmentRepository[F]
) extends AttachmentService[F] {
  private val path = "src/main/resources/attachments"

  override def findFileById(id: Long): F[Either[AttachmentFileError, File]] = {
    val result: EitherT[F, AttachmentFileError, File] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Selecting file with id = $id from file system"))
      attachment <- EitherT.fromOptionF(attachmentRepository.findById(AttachmentId(id)), AttachmentNotFound(id))
      file       <- EitherT.liftF[F, AttachmentFileError, File](new File(path + File.separator + attachment.link).pure[F])
      _          <- EitherT.liftF(Logger[F].info(s"File with id = $id and path = ${file.getAbsolutePath} selected successfully"))
    } yield file

    result.value
  }

  override def deleteFile(id: Long): F[Either[AttachmentFileError, Boolean]] = {
    val result: EitherT[F, AttachmentFileError, Boolean] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Deleting file with id = $id from file system"))
      attachment <- EitherT.fromOptionF(attachmentRepository.findById(AttachmentId(id)), AttachmentNotFound(id))

      isFileDeleted <- EitherT.liftF(Files.deleteIfExists(Paths.get(path + File.separator + attachment.link)).pure[F])
      attachmentDeleted <-
        if (!isFileDeleted) EitherT.liftF[F, AttachmentFileError, Int](0.pure[F])
        else EitherT.liftF[F, AttachmentFileError, Int](attachmentRepository.delete(attachment.id))
      _ <- EitherT.liftF(Logger[F].info(s"File with id = $id delete status: ${attachmentDeleted != 0}"))
    } yield attachmentDeleted != 0

    result.value
  }
}
