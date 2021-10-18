package com.itechart.project.repository.impl

import cats.effect.Bracket
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.item.{DatabaseItem, ItemId}
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.repository.ItemRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieItemRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]) extends ItemRepository[F] {
  private val selectItem: Fragment = fr"SELECT * FROM items"
  private val insertItem: Fragment =
    fr"INSERT INTO items (name, description, amount, price, status, supplier_id)"
  private val setItem:    Fragment = fr"UPDATE items"
  private val deleteItem: Fragment = fr"DELETE FROM items"

  override def all: F[List[DatabaseItem]] = {
    selectItem
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: ItemId): F[Option[DatabaseItem]] = {
    (selectItem ++ fr"WHERE id = $id")
      .query[DatabaseItem]
      .option
      .transact(transactor)
  }

  override def findAllByCategory(category: DatabaseCategory): F[List[DatabaseItem]] = {
    (selectItem ++ fr"WHERE category_id = ${category.id}")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def findAllBySupplier(supplier: DatabaseSupplier): F[List[DatabaseItem]] = {
    (selectItem ++ fr"WHERE supplier_id = ${supplier.id}")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def create(item: DatabaseItem): F[ItemId] = {
    (insertItem ++
      fr"VALUES (${item.name}, ${item.description}, ${item.amount}, " ++
      fr"${item.price}, ${item.status}, ${item.supplier})").update
      .withUniqueGeneratedKeys[ItemId]()
      .transact(transactor)
  }

  override def update(item: DatabaseItem): F[Int] = {
    (setItem ++
      fr"SET name = ${item.name}, description = ${item.description}, " ++
      fr"amount = ${item.amount}, price = ${item.price}, " ++
      fr"status = ${item.status}, supplier_id = ${item.supplier}").update.run
      .transact(transactor)
  }

  override def delete(itemId: ItemId): F[Int] = {
    (deleteItem ++ fr"WHERE id = $itemId").update.run
      .transact(transactor)
  }
}
