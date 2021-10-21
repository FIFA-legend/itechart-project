package com.itechart.project.dto

import com.itechart.project.dto.item.ItemDto
import io.circe.generic.JsonCodec

object cart {

  @JsonCodec case class CartDto(list: List[(ItemDto, Int)])

}
