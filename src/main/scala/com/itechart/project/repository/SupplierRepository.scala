package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId, SupplierName}
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.impl.DoobieSupplierRepository
import doobie.util.transactor.Transactor

trait SupplierRepository[F[_]] {
  def all: F[List[DatabaseSupplier]]
  def findById(id:     SupplierId):       F[Option[DatabaseSupplier]]
  def findByName(name: SupplierName):     F[Option[DatabaseSupplier]]
  def findByUser(user: DatabaseUser):     F[List[DatabaseSupplier]]
  def findByItem(item: DatabaseItem):     F[DatabaseSupplier]
  def create(supplier: DatabaseSupplier): F[SupplierId]
  def update(supplier: DatabaseSupplier): F[Int]
  def delete(id:       SupplierId):       F[Int]
}

object SupplierRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieSupplierRepository[F] =
    new DoobieSupplierRepository[F](transactor)
}
