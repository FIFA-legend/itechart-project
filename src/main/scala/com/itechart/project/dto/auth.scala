package com.itechart.project.dto

import com.itechart.project.domain.user.{EncryptedPassword, UserId, Username}
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe.generic.JsonCodec

object auth {

  final case class ManagerJwtAuth(value: JwtSymmetricAuth)
  final case class CourierJwtAuth(value: JwtSymmetricAuth)
  final case class ClientJwtAuth(value: JwtSymmetricAuth)

  @JsonCodec final case class AuthUser(id: UserId, username: Username)
  @JsonCodec final case class AuthUserWithPassword(
    id:       UserId,
    username: Username,
    password: EncryptedPassword,
    email:    String
  )

  @JsonCodec final case class AuthManagerUser(value: AuthUser)
  @JsonCodec final case class AuthCourierUser(value: AuthUser)
  @JsonCodec final case class AuthClientUser(value: AuthUser)

}
