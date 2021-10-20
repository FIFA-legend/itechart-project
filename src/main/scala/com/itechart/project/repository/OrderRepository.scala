package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.order.{DatabaseOrder, OrderId}
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.impl.DoobieOrderRepository
import doobie.util.transactor.Transactor

trait OrderRepository[F[_]] {
  def all: F[List[DatabaseOrder]]
  def findById(id:     OrderId):       F[Option[DatabaseOrder]]
  def findByUser(user: DatabaseUser):  F[List[DatabaseOrder]]
  def create(order:    DatabaseOrder): F[OrderId]
  def update(order:    DatabaseOrder): F[Int]
}

object OrderRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieOrderRepository[F] =
    new DoobieOrderRepository[F](transactor)
}
