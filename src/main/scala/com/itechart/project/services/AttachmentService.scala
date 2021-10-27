package com.itechart.project.services

import cats.effect.{Blocker, ContextShift, Sync}
import com.itechart.project.repository.{AttachmentRepository, ItemRepository}
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.impl.AttachmentServiceImpl
import io.chrisdavenport.log4cats.Logger

import java.io.File

trait AttachmentService[F[_]] {
  def findFileById(id: Long): F[Either[AttachmentFileError, File]]
  def deleteFile(id:   Long): F[Either[AttachmentFileError, Boolean]]
}

object AttachmentService {
  def of[F[_]: Sync: Logger: ContextShift](
    attachmentRepository: AttachmentRepository[F]
  ): AttachmentService[F] =
    new AttachmentServiceImpl[F](attachmentRepository)
}
