package com.itechart.project.services.error

object AttachmentErrors {

  sealed trait AttachmentFileError extends ValidationError

  object AttachmentFileError {
    // 404
    final case class AttachmentNotFound(attachmentId: Long) extends AttachmentFileError {
      override def message: String =
        s"The file with id `$attachmentId` is not found"
    }

    // 400
    final case class InvalidItemAttachment(itemId: Long) extends AttachmentFileError {
      override def message: String = s"The item with id `$itemId` is not found"
    }
  }

}
