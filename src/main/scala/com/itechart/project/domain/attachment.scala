package com.itechart.project.domain

import com.itechart.project.domain.item.ItemId
import eu.timepit.refined.types.string.NonEmptyString

object attachment {

  final case class AttachmentId(id: Long)

  type Link = NonEmptyString

  final case class DatabaseAttachment(
    id:     AttachmentId,
    link:   Link,
    itemId: ItemId
  )

}
