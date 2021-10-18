package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.item.{DatabaseItem, ItemId}
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.repository.impl.DoobieItemRepository
import doobie.util.transactor.Transactor

trait ItemRepository[F[_]] {
  def all: F[List[DatabaseItem]]
  def findById(id:                ItemId):           F[Option[DatabaseItem]]
  def findAllByCategory(category: DatabaseCategory): F[List[DatabaseItem]]
  def findAllBySupplier(supplier: DatabaseSupplier): F[List[DatabaseItem]]
  def create(item:                DatabaseItem):     F[ItemId]
  def update(item:                DatabaseItem):     F[Int]
  def delete(itemId:              ItemId):           F[Int]
}

object ItemRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieItemRepository[F] =
    new DoobieItemRepository[F](transactor)
}
