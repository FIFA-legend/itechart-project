package com.itechart.project.domain

import com.itechart.project.domain.item.Item
import com.itechart.project.domain.subscription.{Category, Supplier}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.{refineV, W}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

object user {

  @JsonCodec final case class UserId(value: Long)

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

  implicit val emailEncoder: Encoder[Email] = Encoder.encodeString.contramap[Email](_.toString)
  implicit val emailDecoder: Decoder[Email] = Decoder.decodeString.emap[Email](str => refineV(str))

  @JsonCodec final case class AuthorizedUser(
    id:                   UserId,
    username:             Username,
    password:             EncryptedPassword,
    email:                Email,
    role:                 Role,
    availableItems:       List[Item],
    subscribedSuppliers:  List[Supplier],
    subscribedCategories: List[Category]
  )

  final case class QueryUser(
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
