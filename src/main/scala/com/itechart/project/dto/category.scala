package com.itechart.project.dto

import io.circe.generic.JsonCodec

object category {

  @JsonCodec case class CategoryDto(id: Long, name: String)

}
