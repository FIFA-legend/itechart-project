package com.itechart.project.domain

import com.itechart.project.domain.cart.Quantity
import squants.Money

object order {

  final case class OrderId(value: Long)

  trait DeliveryStatus

  object DeliveryStatus {
    final case object Ordered extends DeliveryStatus
    final case object Assigned extends DeliveryStatus
    final case object Delivered extends DeliveryStatus
  }

  final case class Order(
    id:    OrderId,
    items: Map[Long, Quantity],
    total: Money
  )

}
