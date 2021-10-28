package com.itechart.project.dto

import com.itechart.project.domain.item.AvailabilityStatus
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.supplier.SupplierDto
import io.circe.generic.JsonCodec

object item {

  @JsonCodec final case class AttachmentIdDto(id: Long)

  @JsonCodec final case class ItemDto(
    id:          Long,
    name:        String,
    description: String,
    amount:      Int,
    price:       Double,
    status:      AvailabilityStatus,
    supplier:    SupplierDto,
    categories:  List[CategoryDto],
    attachments: List[AttachmentIdDto]
  )

  @JsonCodec final case class FilterItemDto(
    name:        Option[String],
    description: Option[String],
    minPrice:    Option[Double],
    maxPrice:    Option[Double],
    suppliers:   List[SupplierDto],
    categories:  List[CategoryDto]
  )

}
