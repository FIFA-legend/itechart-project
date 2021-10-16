package com.itechart.project.repository.impl

import cats.effect.Bracket
import com.itechart.project.domain.subscription.Supplier
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

  override def all: F[List[Supplier]] = {
    selectSupplier
      .query[Supplier]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: Long): F[Option[Supplier]] = {
    (selectSupplier ++ fr"WHERE id = $id")
      .query[Supplier]
      .option
      .transact(transactor)
  }

  override def create(supplier: Supplier): F[Long] = {
    (insertSupplier ++ fr"VALUES (${supplier.name})").update
      .withUniqueGeneratedKeys[Long]()
      .transact(transactor)
  }

  override def update(supplier: Supplier): F[Int] = {
    (setSupplier ++ fr"SET name = ${supplier.name} WHERE id = ${supplier.id}").update.run
      .transact(transactor)
  }

  override def delete(id: Long): F[Int] = {
    (deleteSupplier ++ fr"WHERE id = $id").update.run
      .transact(transactor)
  }
}
