package com.itechart.project.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import io.circe.generic.JsonCodec

object user {

  @JsonCodec case class UserId(value: Long)

  @JsonCodec final case class Username(value: String)

  @JsonCodec final case class Password(value: String)

  @JsonCodec final case class EncryptedPassword(value: String)

  type Email = String Refined MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]

  sealed trait Role extends EnumEntry

  object Role extends Enum[Role] with CirceEnum[Role] {
    val values: IndexedSeq[Role] = findValues

    final case object Manager extends Role
    final case object Courier extends Role
    final case object Client extends Role
  }

  final case class DatabaseUser(
    id:       UserId,
    username: Username,
    password: EncryptedPassword,
    email:    Email,
    role:     Role
  )

}
