package com.itechart.project.repository.impl

import cats.effect.Bracket
import com.itechart.project.domain.category.{CategoryId, DatabaseCategory}
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.CategoryRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieCategoryRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F])
  extends CategoryRepository[F] {
  private val selectCategory: Fragment = fr"SELECT * FROM categories"
  private val insertCategory: Fragment = fr"INSERT INTO categories (name)"
  private val setCategory:    Fragment = fr"UPDATE categories"
  private val deleteCategory: Fragment = fr"DELETE FROM categories"

  override def all: F[List[DatabaseCategory]] = {
    selectCategory
      .query[DatabaseCategory]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: CategoryId): F[Option[DatabaseCategory]] = {
    (selectCategory ++ fr"WHERE id = $id")
      .query[DatabaseCategory]
      .option
      .transact(transactor)
  }

  override def findByUser(user: DatabaseUser): F[List[DatabaseCategory]] = {
    (selectCategory ++ fr"INNER JOIN users_subscriptions_on_categories"
      ++ fr"ON categories.id = users_subscriptions_on_categories.category_id"
      ++ fr"WHERE users_subscriptions_on_categories.user_id = ${user.id}")
      .query[DatabaseCategory]
      .to[List]
      .transact(transactor)
  }

  override def findByItem(item: DatabaseItem): F[List[DatabaseCategory]] = {
    (selectCategory ++ fr"INNER JOIN items_categories"
      ++ fr"ON categories.id = items_categories.category_id"
      ++ fr"WHERE items_categories.item_id = ${item.id}")
      .query[DatabaseCategory]
      .to[List]
      .transact(transactor)
  }

  override def create(category: DatabaseCategory): F[CategoryId] = {
    (insertCategory ++ fr"VALUES (${category.name})").update
      .withUniqueGeneratedKeys[CategoryId]()
      .transact(transactor)
  }

  override def update(category: DatabaseCategory): F[Int] = {
    (setCategory ++ fr"SET name = ${category.name} WHERE id = ${category.id}").update.run
      .transact(transactor)
  }

  override def delete(id: CategoryId): F[Int] = {
    (deleteCategory ++ fr"WHERE id = $id").update.run
      .transact(transactor)
  }
}
