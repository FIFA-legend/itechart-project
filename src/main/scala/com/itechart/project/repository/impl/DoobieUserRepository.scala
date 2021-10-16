package com.itechart.project.repository.impl

import cats.effect.Bracket
import cats.implicits.toFunctorOps
import com.itechart.project.domain.user.{AuthorizedUser, Email, EncryptedPassword, QueryUser, Role, UserId, Username}
import com.itechart.project.repository.impl.meta.MetaImplicits._
import com.itechart.project.http.auth.users.UserWithPassword
import com.itechart.project.repository.UserRepository
import doobie.Transactor
import doobie.implicits._

class DoobieUserRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]) extends UserRepository[F] {
  override def find(username: Username): F[Option[UserWithPassword]] = {
    val fragment = fr"SELECT * FROM users WHERE username = $username"
    fragment
      .query[QueryUser]
      .option
      .transact(transactor)
      .map {
        case Some(user) => Some(UserWithPassword(user.id, user.username, user.password))
        case _          => None
      }

  }

  override def create(username: Username, password: EncryptedPassword, email: Email): F[UserId] = {
    val fragment =
      fr"INSERT INTO users (username, password, email) VALUES ($username, $password, $email)"
    fragment.update.withUniqueGeneratedKeys[UserId]("id").transact(transactor)
  }
}
