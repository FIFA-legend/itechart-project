package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.order.{Order, OrderId}
import com.itechart.project.domain.user.AuthorizedUser
import com.itechart.project.repository.impl.DoobieOrderRepository
import doobie.util.transactor.Transactor

trait OrderRepository[F[_]] {
  def all: F[List[Order]]
  def findById(id:        OrderId):        F[Option[Order]]
  def findAllByUser(user: AuthorizedUser): F[List[Order]]
  def create(order:       Order):          F[OrderId]
  def update(order:       Order):          F[Int]
  def delete(orderId:     OrderId):        F[Int]
}

object OrderRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieOrderRepository[F] =
    new DoobieOrderRepository[F](transactor)
}
