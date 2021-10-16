package com.itechart.project.repository.impl

import cats.effect.Bracket
import cats.syntax.all._
import com.itechart.project.domain.order.{Order, OrderId, QueryItem, QueryOrder}
import com.itechart.project.domain.user.AuthorizedUser
import com.itechart.project.repository.OrderRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieOrderRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]) extends OrderRepository[F] {
  private val selectOrder:     Fragment = fr"SELECT * FROM orders"
  private val selectOrderItem: Fragment = fr"SELECT * FROM orders_items"

  override def all: F[List[Order]] = {
    for {
      queryOrders <- selectOrder
        .query[QueryOrder]
        .to[List]
        .transact(transactor)
      orders <- queryOrders.map(findAllOrderItems).sequence
    } yield orders
  }

  override def findById(id: OrderId): F[Option[Order]] = {
    for {
      queryOrder <- (selectOrder ++ fr"WHERE id = $id")
        .query[QueryOrder]
        .option
        .transact(transactor)
      order <- queryOrder.map(findAllOrderItems).sequence
    } yield order
  }

  override def findAllByUser(user: AuthorizedUser): F[List[Order]] = {
    for {
      userQueryOrders <- (selectOrder ++ fr"WHERE user_id = ${user.id}")
        .query[QueryOrder]
        .to[List]
        .transact(transactor)
      userOrders <- userQueryOrders.map(findAllOrderItems).sequence
    } yield userOrders
  }

  override def create(order: Order): F[OrderId] = ???

  override def update(order: Order): F[Int] = ???

  override def delete(orderId: OrderId): F[Int] = ???

  private def findAllOrderItems(ord: QueryOrder): F[Order] = {
    for {
      queryItems <- (selectOrderItem ++ fr"WHERE order_id = ${ord.id}")
        .query[QueryItem]
        .to[List]
        .transact(transactor)
      items = queryItems.map(item => (item.itemId, item.quantity)).toMap
    } yield Order(ord.id, ord.total, ord.status, items)
  }
}
