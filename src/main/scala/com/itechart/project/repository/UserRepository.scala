package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.auth.{Email, EncryptedPassword, Role, UserId, Username}
import com.itechart.project.http.auth.users.UserWithPassword
import com.itechart.project.repository.impl.DoobieUserRepository
import doobie.util.transactor.Transactor

trait UserRepository[F[_]] {
  def find(username:   Username): F[Option[UserWithPassword]]
  def create(username: Username, password: EncryptedPassword, email: Email): F[UserId]
}

object UserRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieUserRepository[F] = new DoobieUserRepository[F](transactor)
}
