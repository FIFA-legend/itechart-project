package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.domain.attachment.AttachmentId
import com.itechart.project.repository.AttachmentRepository
import com.itechart.project.services.error.AttachmentErrors.AttachmentFileError
import com.itechart.project.services.impl.AttachmentServiceImpl
import io.chrisdavenport.log4cats.Logger
import org.http4s.multipart.Multipart

import java.io.File

trait AttachmentService[F[_]] {
  def findFileById(id:      Long):         F[Either[AttachmentFileError, File]]
  def createFile(multipart: Multipart[F]): F[Either[AttachmentFileError, AttachmentId]]
  def deleteFile(id:        Long):         F[Either[AttachmentFileError, Boolean]]
}

object AttachmentService {
  def of[F[_]: Sync: Logger](attachmentRepository: AttachmentRepository[F]): AttachmentService[F] =
    new AttachmentServiceImpl[F](attachmentRepository)
}
