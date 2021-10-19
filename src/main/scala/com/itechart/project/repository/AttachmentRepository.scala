package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.attachment.{AttachmentId, DatabaseAttachment}
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.repository.impl.DoobieAttachmentRepository
import doobie.util.transactor.Transactor

trait AttachmentRepository[F[_]] {
  def findById(id:       AttachmentId):       F[Option[DatabaseAttachment]]
  def findByItem(item:   DatabaseItem):       F[List[DatabaseAttachment]]
  def create(attachment: DatabaseAttachment): F[AttachmentId]
  def delete(id:         AttachmentId):       F[Int]
}

object AttachmentRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieAttachmentRepository[F] =
    new DoobieAttachmentRepository[F](transactor)
}
