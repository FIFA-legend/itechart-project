package com.itechart.project.dto

import com.itechart.project.domain.user.Role
import com.itechart.project.dto.cart.SimpleItemDto
import com.itechart.project.dto.user.UserDto
import io.circe.generic.JsonCodec

object group {

  @JsonCodec final case class SimpleUserDto(
    id:       Long,
    username: String,
    email:    String,
    role:     Role
  )

  @JsonCodec final case class GroupDto(
    id:    Long,
    name:  String,
    users: List[SimpleUserDto],
    items: List[SimpleItemDto]
  )

}
