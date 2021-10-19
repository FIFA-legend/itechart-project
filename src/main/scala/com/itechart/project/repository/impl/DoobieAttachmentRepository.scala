package com.itechart.project.repository.impl

import cats.effect.Bracket
import com.itechart.project.domain.attachment.{AttachmentId, DatabaseAttachment}
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.repository.AttachmentRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieAttachmentRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F])
  extends AttachmentRepository[F] {
  private val selectAttachment: Fragment = fr"SELECT * FROM attachments_to_items"
  private val insertAttachment: Fragment = fr"INSERT INTO attachments_to_items (link, item_id)"
  private val deleteAttachment: Fragment = fr"DELETE FROM attachments_to_items"

  override def findById(id: AttachmentId): F[Option[DatabaseAttachment]] = {
    (selectAttachment ++ fr"WHERE id = $id")
      .query[DatabaseAttachment]
      .option
      .transact(transactor)
  }

  override def findByItem(item: DatabaseItem): F[List[DatabaseAttachment]] = {
    (selectAttachment ++ fr"WHERE item_id = ${item.id}")
      .query[DatabaseAttachment]
      .to[List]
      .transact(transactor)
  }

  override def create(attachment: DatabaseAttachment): F[AttachmentId] = {
    (insertAttachment ++ fr"VALUES (${attachment.link}, ${attachment.itemId})").update
      .withUniqueGeneratedKeys[AttachmentId]()
      .transact(transactor)
  }

  override def delete(id: AttachmentId): F[Int] = {
    (deleteAttachment ++ fr"WHERE id = $id").update.run
      .transact(transactor)
  }
}
