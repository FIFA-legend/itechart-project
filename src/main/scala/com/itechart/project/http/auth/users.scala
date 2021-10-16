package com.itechart.project.http.auth

import com.itechart.project.domain.user.{EncryptedPassword, UserId, Username}
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe.generic.JsonCodec

object users {

  final case class ManagerJwtAuth(value: JwtSymmetricAuth)
  final case class CourierJwtAuth(value: JwtSymmetricAuth)
  final case class ClientJwtAuth(value: JwtSymmetricAuth)

  @JsonCodec case class User(id: UserId, username: Username)
  @JsonCodec final case class UserWithPassword(id: UserId, username: Username, password: EncryptedPassword)

  @JsonCodec final case class ManagerUser(value: User)
  @JsonCodec final case class CourierUser(value: User)
  @JsonCodec final case class ClientUser(value: User)

}
