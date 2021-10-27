package com.itechart.project.dto

import io.circe.generic.JsonCodec

object cart {

  @JsonCodec final case class CartDto(items: List[SingleCartDto])

  @JsonCodec final case class SingleCartDto(id: Long, quantity: Int, item: CartItemDto, orderId: Option[Long])

  @JsonCodec final case class CartItemDto(id: Long, name: String, description: String, price: Double)

}
