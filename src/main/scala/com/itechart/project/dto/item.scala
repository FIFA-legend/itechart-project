package com.itechart.project.dto

import com.itechart.project.domain.item.AvailabilityStatus
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.supplier.SupplierDto
import io.circe.generic.JsonCodec

object item {

  @JsonCodec case class ItemDto(
    id:          Long,
    name:        String,
    description: String,
    amount:      Int,
    price:       Double,
    status:      AvailabilityStatus,
    supplier:    SupplierDto,
    categories:  List[CategoryDto]
  )

}
