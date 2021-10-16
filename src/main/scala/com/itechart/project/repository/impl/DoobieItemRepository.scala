package com.itechart.project.repository.impl

import cats.effect.Bracket
import com.itechart.project.domain.item.{Item, ItemId}
import com.itechart.project.domain.subscription.{Category, Supplier}
import com.itechart.project.repository.ItemRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieItemRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]) extends ItemRepository[F] {
  private val selectItem: Fragment = fr"SELECT * FROM items"
  private val insertItem: Fragment =
    fr"INSERT INTO items (name, description, amount, price, status, category_id, supplier_id)"
  private val setItem:    Fragment = fr"UPDATE items"
  private val deleteItem: Fragment = fr"DELETE FROM items"

  override def all: F[List[Item]] = {
    selectItem
      .query[Item]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: ItemId): F[Option[Item]] = {
    (selectItem ++ fr"WHERE id = $id")
      .query[Item]
      .option
      .transact(transactor)
  }

  override def findAllByCategory(category: Category): F[List[Item]] = {
    (selectItem ++ fr"WHERE category_id = ${category.id}")
      .query[Item]
      .to[List]
      .transact(transactor)
  }

  override def findAllBySupplier(supplier: Supplier): F[List[Item]] = {
    (selectItem ++ fr"WHERE supplier_id = ${supplier.id}")
      .query[Item]
      .to[List]
      .transact(transactor)
  }

  override def create(item: Item): F[ItemId] = {
    (insertItem ++
      fr"VALUES (${item.name}, ${item.description}, ${item.amount}, " ++
      fr"${item.price}, ${item.status}, ${item.category}, ${item.supplier})").update
      .withUniqueGeneratedKeys[ItemId]()
      .transact(transactor)
  }

  override def update(item: Item): F[Int] = {
    (setItem ++
      fr"SET name = ${item.name}, description = ${item.description}, " ++
      fr"amount = ${item.amount}, price = ${item.price}, " ++
      fr"status = ${item.status}, category_id = ${item.category}, supplier_id = ${item.supplier}").update.run
      .transact(transactor)
  }

  override def delete(itemId: ItemId): F[Int] = {
    (deleteItem ++ fr"WHERE id = $itemId").update.run
      .transact(transactor)
  }
}
