package com.itechart.project.dto

import com.itechart.project.domain.order.DeliveryStatus
import com.itechart.project.dto.cart.CartDto
import io.circe.generic.JsonCodec

object order {

  @JsonCodec case class OrderDto(
    id:      Long,
    total:   Double,
    address: String,
    status:  DeliveryStatus,
    cart:    CartDto
  )

}
