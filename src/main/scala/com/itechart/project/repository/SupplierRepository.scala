package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.repository.impl.DoobieSupplierRepository
import doobie.util.transactor.Transactor

trait SupplierRepository[F[_]] {
  def all: F[List[DatabaseSupplier]]
  def findById(id:     Long):             F[Option[DatabaseSupplier]]
  def create(supplier: DatabaseSupplier): F[Long]
  def update(supplier: DatabaseSupplier): F[Int]
  def delete(id:       Long):             F[Int]
}

object SupplierRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieSupplierRepository[F] =
    new DoobieSupplierRepository[F](transactor)
}
