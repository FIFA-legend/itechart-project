package com.itechart.project.domain

import com.itechart.project.domain.category.CategoryId
import com.itechart.project.domain.supplier.SupplierId
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.refineV
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.{Decoder, Encoder}
import squants.market.{Money, USD}

import scala.util.Try

object item {

  final case class ItemId(value: Long)

  type ItemName = NonEmptyString

  type ItemDescription = NonEmptyString

  type Amount = Int Refined GreaterEqual[0]

  sealed trait AvailabilityStatus extends EnumEntry

  object AvailabilityStatus extends Enum[AvailabilityStatus] with CirceEnum[AvailabilityStatus] {
    val values: IndexedSeq[AvailabilityStatus] = findValues

    final case object InProcessing extends AvailabilityStatus
    final case object Available extends AvailabilityStatus
    final case object NotAvailable extends AvailabilityStatus
  }

  final case class DatabaseItem(
    id:          ItemId,
    name:        ItemName,
    description: ItemDescription,
    amount:      Amount,
    price:       Money,
    status:      AvailabilityStatus,
    supplier:    SupplierId
  )

  final case class DatabaseItemFilter(
    name:        Option[String],
    description: Option[String],
    minPrice:    Option[Double],
    maxPrice:    Option[Double],
    suppliers:   List[SupplierId],
    categories:  List[CategoryId]
  )

}
