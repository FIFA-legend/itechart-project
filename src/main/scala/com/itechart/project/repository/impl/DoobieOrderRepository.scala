package com.itechart.project.repository.impl

import cats.effect.MonadCancelThrow
import com.itechart.project.domain.order.{DatabaseOrder, OrderId}
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.OrderRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieOrderRepository[F[_]: MonadCancelThrow](transactor: Transactor[F]) extends OrderRepository[F] {
  private val selectOrder: Fragment = fr"SELECT id, total, address, status, user_id FROM orders"
  private val insertOrder: Fragment = fr"INSERT INTO orders (total, address, user_id)"
  private val setOrder:    Fragment = fr"UPDATE orders"

  override def all: F[List[DatabaseOrder]] = {
    selectOrder
      .query[DatabaseOrder]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: OrderId): F[Option[DatabaseOrder]] = {
    (selectOrder ++ fr"WHERE id = $id")
      .query[DatabaseOrder]
      .option
      .transact(transactor)
  }

  override def findByUser(user: DatabaseUser): F[List[DatabaseOrder]] = {
    (selectOrder ++ fr"WHERE user_id = ${user.id}")
      .query[DatabaseOrder]
      .to[List]
      .transact(transactor)
  }

  override def create(order: DatabaseOrder): F[OrderId] = {
    (insertOrder ++ fr"VALUES (${order.total}, ${order.address}, ${order.userId})").update
      .withUniqueGeneratedKeys[OrderId]("id")
      .transact(transactor)
  }

  override def update(order: DatabaseOrder): F[Int] = {
    (setOrder ++ fr"SET status = ${order.status} WHERE id = ${order.id}").update.run.transact(transactor)
  }
}
