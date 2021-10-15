package com.itechart.project.domain

import eu.timepit.refined.{refineV, W}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec

object auth {

  @JsonCodec final case class UserId(value: Long)

  @JsonCodec final case class Username(value: String)

  @JsonCodec final case class Password(value: String)

  @JsonCodec final case class EncryptedPassword(value: String)

  type Email = String Refined MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]

  sealed trait Role

  object Role {
    final case object Manager extends Role
    final case object Courier extends Role
    final case object Client extends Role
  }

  implicit val emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](_.toString)
  implicit val emailDecoder: Decoder[Email] = Decoder.decodeString.emap[Email](str => refineV(str))

  @JsonCodec final case class AuthorizedUser(
    id:       UserId,
    username: Username,
    password: EncryptedPassword,
    email:    Email,
    role:     Role
  )

  sealed trait UserAuthenticationError extends Throwable

  object UserAuthenticationError {
    final case class InvalidPassword(username: Username) extends UserAuthenticationError
    final case class UsernameInUse(username: Username) extends UserAuthenticationError
    final case class UserNotFound(username: Username) extends UserAuthenticationError
    final case object UnsupportedOperation extends UserAuthenticationError
    final case object TokenNotFound extends UserAuthenticationError
  }

}
