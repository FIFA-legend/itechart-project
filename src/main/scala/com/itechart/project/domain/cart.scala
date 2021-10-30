package com.itechart.project.domain

import com.itechart.project.domain.item.{DatabaseItem, ItemId}
import com.itechart.project.domain.order.OrderId
import com.itechart.project.domain.user.UserId
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.GreaterEqual
import squants.Money

object cart {

  final case class CartId(value: Long)

  type Quantity = Int Refined GreaterEqual[1]

  final case class DatabaseCart(
    id:       CartId,
    quantity: Quantity,
    itemId:   ItemId,
    userId:   UserId,
    orderId:  Option[OrderId]
  )

}
