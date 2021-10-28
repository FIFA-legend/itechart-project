package com.itechart.project.repository.impl

import cats.effect.Bracket
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId, SupplierName}
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.SupplierRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieSupplierRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F])
  extends SupplierRepository[F] {
  private val selectSupplier: Fragment = fr"SELECT * FROM suppliers"
  private val insertSupplier: Fragment = fr"INSERT INTO suppliers (name)"
  private val setSupplier:    Fragment = fr"UPDATE suppliers"
  private val deleteSupplier: Fragment = fr"DELETE FROM suppliers"

  override def all: F[List[DatabaseSupplier]] = {
    selectSupplier
      .query[DatabaseSupplier]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: SupplierId): F[Option[DatabaseSupplier]] = {
    (selectSupplier ++ fr"WHERE id = $id")
      .query[DatabaseSupplier]
      .option
      .transact(transactor)
  }

  def findByName(name: SupplierName): F[Option[DatabaseSupplier]] = {
    (selectSupplier ++ fr"WHERE name = $name")
      .query[DatabaseSupplier]
      .option
      .transact(transactor)
  }

  override def findByUser(user: DatabaseUser): F[List[DatabaseSupplier]] = {
    (selectSupplier ++ fr"INNER JOIN users_subscriptions_on_suppliers"
      ++ fr"ON suppliers.id = users_subscriptions_on_suppliers.supplier_id"
      ++ fr"WHERE users_subscriptions_on_suppliers.user_id = ${user.id}")
      .query[DatabaseSupplier]
      .to[List]
      .transact(transactor)
  }

  def findByItem(item: DatabaseItem): F[DatabaseSupplier] = {
    (selectSupplier ++ fr"WHERE id = ${item.supplier}")
      .query[DatabaseSupplier]
      .unique
      .transact(transactor)
  }

  override def create(supplier: DatabaseSupplier): F[SupplierId] = {
    (insertSupplier ++ fr"VALUES (${supplier.name})").update
      .withUniqueGeneratedKeys[SupplierId]("id")
      .transact(transactor)
  }

  override def update(supplier: DatabaseSupplier): F[Int] = {
    (setSupplier ++ fr"SET name = ${supplier.name} WHERE id = ${supplier.id}").update.run
      .transact(transactor)
  }

  override def delete(id: SupplierId): F[Int] = {
    (deleteSupplier ++ fr"WHERE id = $id").update.run
      .transact(transactor)
  }
}
