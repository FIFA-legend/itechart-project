package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.cart.{CartId, DatabaseCart}
import com.itechart.project.domain.order.DatabaseOrder
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.impl.DoobieCartRepository
import doobie.util.transactor.Transactor

trait CartRepository[F[_]] {
  def findById(id:                 CartId):        F[Option[DatabaseCart]]
  def findCurrentCartsByUser(user: DatabaseUser):  F[List[DatabaseCart]]
  def findByOrder(order:           DatabaseOrder): F[List[DatabaseCart]]
  def create(cart:                 DatabaseCart):  F[CartId]
  def update(cart:                 DatabaseCart):  F[Int]
  def delete(id:                   CartId):        F[Int]
}

object CartRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieCartRepository[F] =
    new DoobieCartRepository[F](transactor)
}
