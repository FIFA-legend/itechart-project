package com.itechart.project.domain

import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.supplier.DatabaseSupplier
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

object user {

  final case class UserId(value: Long)

  final case class Username(value: String)

  final case class Password(value: String)

  final case class EncryptedPassword(value: String)

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

  final case class AuthorizedUser(
    id:                   UserId,
    username:             Username,
    password:             EncryptedPassword,
    email:                Email,
    role:                 Role,
    availableItems:       List[DatabaseItem],
    subscribedSuppliers:  List[DatabaseSupplier],
    subscribedCategories: List[DatabaseCategory]
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
