package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.subscription.Category
import com.itechart.project.repository.impl.DoobieSupplierRepository
import doobie.util.transactor.Transactor

trait CategoryRepository[F[_]] {
  def all: F[List[Category]]
  def findById(id:     Long):     F[Option[Category]]
  def create(category: Category): F[Long]
  def update(category: Category): F[Int]
  def delete(id:       Long):     F[Int]
}

object CategoryRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieSupplierRepository[F] =
    new DoobieSupplierRepository[F](transactor)
}
