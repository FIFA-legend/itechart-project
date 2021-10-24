package com.itechart.project.services.error

import scala.util.control.NoStackTrace

object AttachmentErrors {

  sealed trait AttachmentFileError extends RuntimeException with NoStackTrace {
    def message: String
  }

  object AttachmentFileError {
    // 404
    final case class AttachmentNotFound(attachmentId: Long) extends AttachmentFileError {
      override def message: String =
        s"The file with id `$attachmentId` is not found"
    }
  }

}
