package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.item.{Item, ItemId}
import com.itechart.project.domain.subscription.{Category, Supplier}
import com.itechart.project.repository.impl.DoobieItemRepository
import doobie.util.transactor.Transactor

trait ItemRepository[F[_]] {
  def all: F[List[Item]]
  def findById(id:                ItemId):   F[Option[Item]]
  def findAllByCategory(category: Category): F[List[Item]]
  def findAllBySupplier(supplier: Supplier): F[List[Item]]
  def create(item:                Item):     F[ItemId]
  def update(item:                Item):     F[Int]
  def delete(itemId:              ItemId):   F[Int]
}

object ItemRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieItemRepository[F] =
    new DoobieItemRepository[F](transactor)
}
