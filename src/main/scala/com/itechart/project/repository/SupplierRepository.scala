package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.subscription.Supplier
import com.itechart.project.repository.impl.DoobieSupplierRepository
import doobie.util.transactor.Transactor

trait SupplierRepository[F[_]] {
  def all: F[List[Supplier]]
  def findById(id:     Long):     F[Option[Supplier]]
  def create(supplier: Supplier): F[Long]
  def update(supplier: Supplier): F[Int]
  def delete(id:       Long):     F[Int]
}

object SupplierRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieSupplierRepository[F] =
    new DoobieSupplierRepository[F](transactor)
}
