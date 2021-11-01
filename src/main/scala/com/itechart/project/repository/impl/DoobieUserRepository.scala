package com.itechart.project.repository.impl

import cats.effect.MonadCancelThrow
import cats.implicits._
import com.itechart.project.domain.category.DatabaseCategory
import com.itechart.project.domain.group.DatabaseGroup
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.supplier.DatabaseSupplier
import com.itechart.project.domain.user.{DatabaseUser, Email, UserId, Username}
import com.itechart.project.repository.impl.meta.MetaImplicits._
import com.itechart.project.repository.UserRepository
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieUserRepository[F[_]: MonadCancelThrow](transactor: Transactor[F]) extends UserRepository[F] {
  private val selectUser: Fragment = fr"SELECT * FROM users"
  private val insertUser: Fragment = fr"INSERT INTO users (username, password, email)"
  private val setUser:    Fragment = fr"UPDATE users"
  private val deleteUser: Fragment = fr"DELETE FROM users"
  private val exists:     Fragment = fr"SELECT EXISTS"

  private val insertCategoryToUser   = fr"INSERT INTO users_subscriptions_on_categories (user_id, category_id)"
  private val deleteCategoryFromUser = fr"DELETE FROM users_subscriptions_on_categories"

  private val insertSupplierToUser   = fr"INSERT INTO users_subscriptions_on_suppliers (user_id, supplier_id)"
  private val deleteSupplierFromUser = fr"DELETE FROM users_subscriptions_on_suppliers"

  override def all: F[List[DatabaseUser]] = {
    selectUser
      .query[DatabaseUser]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: UserId): F[Option[DatabaseUser]] = {
    (selectUser ++ fr"WHERE id = $id")
      .query[DatabaseUser]
      .option
      .transact(transactor)
  }

  override def findByUsername(username: Username): F[Option[DatabaseUser]] = {
    (selectUser ++ fr"WHERE username = $username")
      .query[DatabaseUser]
      .option
      .transact(transactor)
  }

  def findByEmail(email: Email): F[Option[DatabaseUser]] = {
    (selectUser ++ fr"WHERE email = $email")
      .query[DatabaseUser]
      .option
      .transact(transactor)
  }

  override def findByItem(item: DatabaseItem): F[List[DatabaseUser]] = {
    (selectUser ++ fr"INNER JOIN items_to_single_user"
      ++ fr"ON users.id = items_to_single_user.user_id"
      ++ fr"WHERE items_to_single_user.item_id = ${item.id}")
      .query[DatabaseUser]
      .to[List]
      .transact(transactor)
  }

  override def findByGroup(group: DatabaseGroup): F[List[DatabaseUser]] = {
    (selectUser ++ fr"INNER JOIN users_to_groups"
      ++ fr"ON users.id = users_to_groups.user_id"
      ++ fr"WHERE users_to_groups.group_id = ${group.id}")
      .query[DatabaseUser]
      .to[List]
      .transact(transactor)
  }

  override def findByCategory(category: DatabaseCategory): F[List[DatabaseUser]] = {
    (selectUser ++ fr"INNER JOIN users_subscriptions_on_categories"
      ++ fr"ON users.id = users_subscriptions_on_categories.user_id"
      ++ fr"WHERE users_subscriptions_on_categories.category_id = ${category.id}")
      .query[DatabaseUser]
      .to[List]
      .transact(transactor)
  }

  override def findBySupplier(supplier: DatabaseSupplier): F[List[DatabaseUser]] = {
    (selectUser ++ fr"INNER JOIN users_subscriptions_on_suppliers"
      ++ fr"ON users.id = users_subscriptions_on_suppliers.user_id"
      ++ fr"WHERE users_subscriptions_on_suppliers.supplier_id = ${supplier.id}")
      .query[DatabaseUser]
      .to[List]
      .transact(transactor)
  }

  override def create(user: DatabaseUser): F[UserId] = {
    (insertUser ++ fr"VALUES (${user.username}, ${user.password}, ${user.email})").update
      .withUniqueGeneratedKeys[UserId]("id")
      .transact(transactor)
  }

  override def update(user: DatabaseUser): F[Int] = {
    (setUser ++ fr"SET username = ${user.username}, password = ${user.password},"
      ++ fr"email = ${user.email}, role = ${user.role} WHERE id = ${user.id}").update.run
      .transact(transactor)
  }

  override def delete(id: UserId): F[Int] = {
    (deleteUser ++ fr"WHERE id = $id").update.run.transact(transactor)
  }

  override def isUserSubscribedOnCategory(user: DatabaseUser, category: DatabaseCategory): F[Boolean] = {
    val selected = (exists ++ fr"(SELECT id FROM users_subscriptions_on_categories"
      ++ fr"WHERE user_id = ${user.id} AND category_id = ${category.id})")
      .query[Int]
      .unique
      .transact(transactor)

    for {
      value <- selected
    } yield value == 1
  }

  override def subscribeToCategory(user: DatabaseUser, category: DatabaseCategory): F[Int] = {
    (insertCategoryToUser ++ fr"VALUES (${user.id}, ${category.id})").update.run
      .transact(transactor)
  }

  override def unsubscribeFromCategory(user: DatabaseUser, category: DatabaseCategory): F[Int] = {
    (deleteCategoryFromUser ++ fr"WHERE user_id = ${user.id} AND category_id = ${category.id}").update.run
      .transact(transactor)
  }

  override def isUserSubscribedOnSupplier(user: DatabaseUser, supplier: DatabaseSupplier): F[Boolean] = {
    val selected = (exists ++ fr"(SELECT id FROM users_subscriptions_on_suppliers"
      ++ fr"WHERE user_id = ${user.id} AND supplier_id = ${supplier.id})")
      .query[Int]
      .unique
      .transact(transactor)

    for {
      value <- selected
    } yield value == 1
  }

  override def subscribeToSupplier(user: DatabaseUser, supplier: DatabaseSupplier): F[Int] = {
    (insertSupplierToUser ++ fr"VALUES (${user.id}, ${supplier.id})").update.run
      .transact(transactor)
  }

  override def unsubscribeFromSupplier(user: DatabaseUser, supplier: DatabaseSupplier): F[Int] = {
    (deleteSupplierFromUser ++ fr"WHERE user_id = ${user.id} AND supplier_id = ${supplier.id}").update.run
      .transact(transactor)
  }
}
