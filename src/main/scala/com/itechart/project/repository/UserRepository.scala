package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.group.DatabaseGroup
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.domain.user.{DatabaseUser, Email, UserId, Username}
import com.itechart.project.repository.impl.DoobieUserRepository
import doobie.util.transactor.Transactor

trait UserRepository[F[_]] {
  def all: F[List[DatabaseUser]]
  def findById(id:                     UserId):                F[Option[DatabaseUser]]
  def findByUsername(username:         Username):              F[Option[DatabaseUser]]
  def findByEmail(email:               Email):                 F[Option[DatabaseUser]]
  def findByItem(item:                 DatabaseItem):          F[List[DatabaseUser]]
  def findByGroup(group:               DatabaseGroup):         F[List[DatabaseUser]]
  def create(user:                     DatabaseUser):          F[UserId]
  def update(user:                     DatabaseUser):          F[Int]
  def delete(id:                       UserId): F[Int]
  def isUserSubscribedOnCategory(user: DatabaseUser, category: DatabaseCategory): F[Boolean]
  def subscribeToCategory(user:        DatabaseUser, category: DatabaseCategory): F[Int]
  def unsubscribeFromCategory(user:    DatabaseUser, category: DatabaseCategory): F[Int]
  def isUserSubscribedOnSupplier(user: DatabaseUser, supplier: DatabaseSupplier): F[Boolean]
  def subscribeToSupplier(user:        DatabaseUser, supplier: DatabaseSupplier): F[Int]
  def unsubscribeFromSupplier(user:    DatabaseUser, supplier: DatabaseSupplier): F[Int]
}

object UserRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieUserRepository[F] =
    new DoobieUserRepository[F](transactor)
}
