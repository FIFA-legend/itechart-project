package com.itechart.project.domain

import com.itechart.project.domain.item.{DatabaseItem, ItemId}
import com.itechart.project.domain.order.OrderId
import com.itechart.project.domain.user.UserId
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.GreaterEqual
import squants.Money

object cart {

  final case class CartId(id: Long)

  type Quantity = Int Refined GreaterEqual[1]

  final case class DatabaseCart(
    id:       CartId,
    quantity: Quantity,
    itemId:   ItemId,
    userId:   UserId,
    orderId:  OrderId
  )

  final case class Cart(items: Map[Long, Quantity])

  final case class CartItem(item: DatabaseItem, quantity: Quantity)

  final case class CartTotal(items: List[CartItem], total: Money)

}
