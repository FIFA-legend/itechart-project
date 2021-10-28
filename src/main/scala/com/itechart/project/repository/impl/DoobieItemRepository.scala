package com.itechart.project.repository.impl

import cats.effect.Bracket
import cats.implicits._
import com.itechart.project.domain.cart.DatabaseCart
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.group.DatabaseGroup
import com.itechart.project.domain.item.{AvailabilityStatus, DatabaseItem, DatabaseItemFilter, ItemId}
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.ItemRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie._
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieItemRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]) extends ItemRepository[F] {
  private val selectItem: Fragment = fr"SELECT * FROM items"
  private val insertItem: Fragment =
    fr"INSERT INTO items (name, description, amount, price, status, supplier_id)"
  private val setItem:    Fragment = fr"UPDATE items"
  private val deleteItem: Fragment = fr"DELETE FROM items"

  private val insertItemToSingleUser: Fragment =
    fr"INSERT INTO items_to_single_user (item_id, user_id)"
  private val deleteItemFromSingleUser: Fragment = fr"DELETE FROM items_to_single_user"

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

  def findByStatus(status: AvailabilityStatus): F[List[DatabaseItem]] = {
    (selectItem ++ fr"WHERE status = $status")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def findByCategory(category: DatabaseCategory): F[List[DatabaseItem]] = {
    (selectItem ++ fr"INNER JOIN items_categories"
      ++ fr"ON items.id = items_categories.item_id"
      ++ fr"WHERE items_categories.category_id = ${category.id}")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def findBySupplier(supplier: DatabaseSupplier): F[List[DatabaseItem]] = {
    (selectItem ++ fr"WHERE supplier_id = ${supplier.id}")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def findByUser(user: DatabaseUser): F[List[DatabaseItem]] = {
    (selectItem ++ fr"INNER JOIN items_to_single_user"
      ++ fr"ON items.id = items_to_single_user.item_id"
      ++ fr"WHERE items_to_single_user.user_id = ${user.id}")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  def findByGroup(group: DatabaseGroup): F[List[DatabaseItem]] = {
    (selectItem ++ fr"INNER JOIN items_to_groups"
      ++ fr"ON items.id = items_to_groups.item_id"
      ++ fr"WHERE items_to_groups.group_id = ${group.id}")
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

  override def findByCart(cart: DatabaseCart): F[Option[DatabaseItem]] = {
    (selectItem ++ fr"WHERE id = ${cart.itemId}")
      .query[DatabaseItem]
      .option
      .transact(transactor)
  }

  override def create(item: DatabaseItem): F[ItemId] = {
    (insertItem ++
      fr"VALUES (${item.name}, ${item.description}, ${item.amount}, " ++
      fr"${item.price}, ${item.status}, ${item.supplier})").update
      .withUniqueGeneratedKeys[ItemId]("id")
      .transact(transactor)
  }

  override def update(item: DatabaseItem): F[Int] = {
    (setItem ++
      fr"SET name = ${item.name}, description = ${item.description}, " ++
      fr"amount = ${item.amount}, price = ${item.price}, " ++
      fr"status = ${item.status}, supplier_id = ${item.supplier} WHERE id = ${item.id}").update.run
      .transact(transactor)
  }

  override def delete(itemId: ItemId): F[Int] = {
    (deleteItem ++ fr"WHERE id = $itemId").update.run
      .transact(transactor)
  }

  override def addItemToSingleUser(item: DatabaseItem, user: DatabaseUser): F[Int] = {
    (insertItemToSingleUser ++ fr"VALUES (${item.id}, ${user.id})").update.run
      .transact(transactor)
  }

  override def removeItemFromSingleUser(item: DatabaseItem, user: DatabaseUser): F[Int] = {
    (deleteItemFromSingleUser ++ fr"WHERE item_id = ${item.id} AND user_id = ${user.id}").update.run
      .transact(transactor)
  }

  def filter(filter: DatabaseItemFilter): F[List[DatabaseItem]] = {
    val f1 = filter.name.map(_ + "%").map(n => fr"items.name LIKE $n")
    val f2 = filter.description.map("%" + _ + "%").map(d => fr"items.description LIKE $d")
    val f3 = filter.minPrice.map(min => fr"items.price >= $min")
    val f4 = filter.maxPrice.map(max => fr"items.price <= $max")
    val f5 = filter.suppliers.toNel.map(s => Fragments.in(fr"items.supplier_id", s))
    val f6 = filter.categories.toNel.map(c => Fragments.in(fr"items_categories.category_id", c))

    (selectItem
      ++ fr"INNER JOIN items_categories ON items.id = items_categories.item_id"
      ++ Fragments.whereAndOpt(f1, f2, f3, f4, f5, f6))
      .query[DatabaseItem]
      .to[List]
      .transact(transactor)
  }

}
