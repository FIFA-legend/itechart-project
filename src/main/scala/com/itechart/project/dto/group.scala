package com.itechart.project.dto

import com.itechart.project.dto.item.ItemDto
import com.itechart.project.dto.user.UserDto
import io.circe.generic.JsonCodec

object group {

  @JsonCodec case class GroupDto(
    id:    Long,
    name:  String,
    users: List[UserDto],
    items: List[ItemDto]
  )

}
