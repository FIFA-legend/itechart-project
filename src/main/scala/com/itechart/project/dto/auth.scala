package com.itechart.project.dto

import com.itechart.project.domain.user.{EncryptedPassword, Role, UserId, Username}
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe.generic.JsonCodec

import java.util.UUID

object auth {

  @JsonCodec final case class LoginUser(username: String, password: String)

  final case class UserJwtAuth(value: JwtSymmetricAuth)

  @JsonCodec final case class AuthUser(id: UUID, longId: Long, username: Username, role: Role)

  @JsonCodec final case class AuthUserWithPassword(
    id:       UserId,
    username: Username,
    password: EncryptedPassword,
    email:    String
  )

  @JsonCodec final case class LoggedInUser(value: AuthUser)

}
