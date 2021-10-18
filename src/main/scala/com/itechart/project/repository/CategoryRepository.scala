package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.repository.impl.DoobieSupplierRepository
import doobie.util.transactor.Transactor

trait CategoryRepository[F[_]] {
  def all: F[List[DatabaseCategory]]
  def findById(id:     Long):             F[Option[DatabaseCategory]]
  def create(category: DatabaseCategory): F[Long]
  def update(category: DatabaseCategory): F[Int]
  def delete(id:       Long):             F[Int]
}

object CategoryRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieSupplierRepository[F] =
    new DoobieSupplierRepository[F](transactor)
}
