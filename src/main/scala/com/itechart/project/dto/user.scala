package com.itechart.project.dto

import com.itechart.project.domain.user.Role
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.supplier.SupplierDto
import io.circe.generic.JsonCodec

object user {

  @JsonCodec case class UserDto(
    username: String,
    password: String,
    email:    String,
    role:     Role
  )

  @JsonCodec final case class FullUserDto(
    id:                   Long,
    username:             String,
    email:                String,
    role:                 Role,
    subscribedCategories: List[CategoryDto],
    subscribedSuppliers:  List[SupplierDto]
  )

}
