package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.cart.DatabaseCart
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.group.DatabaseGroup
import com.itechart.project.domain.item.{AvailabilityStatus, DatabaseItem, DatabaseItemFilter, ItemId}
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.impl.DoobieItemRepository
import doobie.util.transactor.Transactor

trait ItemRepository[F[_]] {
  def all: F[List[DatabaseItem]]
  def findById(id:                   ItemId):             F[Option[DatabaseItem]]
  def findByStatus(status:           AvailabilityStatus): F[List[DatabaseItem]]
  def findByCategory(category:       DatabaseCategory):   F[List[DatabaseItem]]
  def findBySupplier(supplier:       DatabaseSupplier):   F[List[DatabaseItem]]
  def findByUser(user:               DatabaseUser):       F[List[DatabaseItem]]
  def findByGroup(group:             DatabaseGroup):      F[List[DatabaseItem]]
  def findByCart(cart:               DatabaseCart):       F[Option[DatabaseItem]]
  def create(item:                   DatabaseItem):       F[ItemId]
  def update(item:                   DatabaseItem):       F[Int]
  def delete(itemId:                 ItemId): F[Int]
  def addItemToSingleUser(item:      DatabaseItem, user: DatabaseUser): F[Int]
  def removeItemFromSingleUser(item: DatabaseItem, user: DatabaseUser): F[Int]
  def filter(itemFilter:             DatabaseItemFilter): F[List[DatabaseItem]]
}

object ItemRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieItemRepository[F] =
    new DoobieItemRepository[F](transactor)
}
