package com.itechart.project.domain

import com.itechart.project.domain.cart.Quantity
import com.itechart.project.domain.item.ItemId
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.types.string.NonEmptyString
import squants.Money

object order {

  final case class OrderId(value: Long)

  type Address = NonEmptyString

  sealed trait DeliveryStatus extends EnumEntry

  object DeliveryStatus extends Enum[DeliveryStatus] with CirceEnum[DeliveryStatus] {
    val values: IndexedSeq[DeliveryStatus] = findValues

    final case object Ordered extends DeliveryStatus
    final case object Assigned extends DeliveryStatus
    final case object Delivered extends DeliveryStatus
  }

  final case class DatabaseOrder(
    id:      OrderId,
    total:   Money,
    address: Address,
    status:  DeliveryStatus
  )

  final case class Order(
    id:     OrderId,
    total:  Money,
    status: DeliveryStatus,
    items:  Map[ItemId, Quantity]
  )

  final case class QueryOrder(id: OrderId, total: Money, status: DeliveryStatus)
  final case class QueryItem(orderId: OrderId, itemId: ItemId, quantity: Quantity)

}
