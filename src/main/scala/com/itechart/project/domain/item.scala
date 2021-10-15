package com.itechart.project.domain

import com.itechart.project.domain.category.Category
import eu.timepit.refined.types.string.NonEmptyString
import squants.Money

object item {

  type ItemName        = NonEmptyString
  type ItemDescription = NonEmptyString
  type ItemSupplier    = NonEmptyString

  final case class ItemId(value: Long)

  trait AvailabilityStatus

  object AvailabilityStatus {
    final case object InProcessing extends AvailabilityStatus
    final case object Available extends AvailabilityStatus
    final case object NotAvailable extends AvailabilityStatus
  }

  final case class Item(
    id:          ItemId,
    name:        ItemName,
    description: ItemDescription,
    supplier:    ItemSupplier,
    price:       Money,
    status:      AvailabilityStatus,
    category:    Category
  )

}
