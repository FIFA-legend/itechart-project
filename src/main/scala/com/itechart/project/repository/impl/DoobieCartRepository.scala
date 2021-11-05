package com.itechart.project.repository.impl

import cats.effect.MonadCancelThrow
import com.itechart.project.domain.cart.{CartId, DatabaseCart}
import com.itechart.project.domain.order.DatabaseOrder
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.CartRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieCartRepository[F[_]: MonadCancelThrow](transactor: Transactor[F]) extends CartRepository[F] {
  private val selectCart: Fragment = fr"SELECT * FROM carts"
  private val insertCart: Fragment = fr"INSERT INTO carts (quantity, item_id, user_id)"
  private val setCart:    Fragment = fr"UPDATE carts"
  private val deleteCart: Fragment = fr"DELETE FROM carts"

  override def findById(id: CartId): F[Option[DatabaseCart]] = {
    (selectCart ++ fr"WHERE id = $id")
      .query[DatabaseCart]
      .option
      .transact(transactor)
  }

  override def findCurrentCartsByUser(user: DatabaseUser): F[List[DatabaseCart]] = {
    (selectCart ++ fr"WHERE user_id = ${user.id} AND order_id IS NULL")
      .query[DatabaseCart]
      .to[List]
      .transact(transactor)
  }

  override def findByOrder(order: DatabaseOrder): F[List[DatabaseCart]] = {
    (selectCart ++ fr"WHERE order_id = ${order.id}")
      .query[DatabaseCart]
      .to[List]
      .transact(transactor)
  }

  override def create(cart: DatabaseCart): F[CartId] = {
    (insertCart ++ fr"VALUES (${cart.quantity}, ${cart.itemId}, ${cart.userId})").update
      .withUniqueGeneratedKeys[CartId]("id")
      .transact(transactor)
  }

  override def update(cart: DatabaseCart): F[Int] = {
    (setCart ++ fr"SET quantity = ${cart.quantity}, item_id = ${cart.itemId},"
      ++ fr"user_id = ${cart.userId}, order_id = ${cart.orderId} WHERE id = ${cart.id}").update.run.transact(transactor)
  }

  override def delete(id: CartId): F[Int] = {
    (deleteCart ++ fr"WHERE id = $id").update.run.transact(transactor)
  }
}
