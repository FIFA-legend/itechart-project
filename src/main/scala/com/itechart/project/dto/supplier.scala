package com.itechart.project.dto

import io.circe.generic.JsonCodec

object supplier {

  @JsonCodec case class SupplierDto(id: Long, name: String)

}
