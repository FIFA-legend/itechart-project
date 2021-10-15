package com.itechart.project.domain

import com.itechart.project.domain.item.Item
import squants.Money

object cart {

  final case class Quantity(value: Int)

  final case class Cart(items: Map[Long, Quantity])

  final case class CartItem(item: Item, quantity: Quantity)

  final case class CartTotal(items: List[CartItem], total: Money)

}
