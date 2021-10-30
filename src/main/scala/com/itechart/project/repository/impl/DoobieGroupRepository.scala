package com.itechart.project.repository.impl

import cats.effect.Bracket
import cats.implicits._
import com.itechart.project.domain.group.{DatabaseGroup, GroupId, GroupName}
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.GroupRepository
import com.itechart.project.repository.impl.meta.MetaImplicits._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment

class DoobieGroupRepository[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F]) extends GroupRepository[F] {
  private val selectGroup: Fragment = fr"SELECT * FROM user_groups"
  private val insertGroup: Fragment = fr"INSERT INTO user_groups (name)"
  private val setGroup:    Fragment = fr"UPDATE user_groups"
  private val deleteGroup: Fragment = fr"DELETE FROM user_groups"
  private val exists:      Fragment = fr"SELECT EXISTS"

  private val insertUserToGroup:   Fragment = fr"INSERT INTO users_to_groups (user_id, group_id)"
  private val deleteUserFromGroup: Fragment = fr"DELETE FROM users_to_groups"

  private val insertItemToGroup:   Fragment = fr"INSERT INTO items_to_groups (item_id, group_id)"
  private val deleteItemFromGroup: Fragment = fr"DELETE FROM items_to_groups"

  override def all: F[List[DatabaseGroup]] = {
    selectGroup
      .query[DatabaseGroup]
      .to[List]
      .transact(transactor)
  }

  override def findById(id: GroupId): F[Option[DatabaseGroup]] = {
    (selectGroup ++ fr"WHERE id = $id")
      .query[DatabaseGroup]
      .option
      .transact(transactor)
  }

  def findByName(name: GroupName): F[Option[DatabaseGroup]] = {
    (selectGroup ++ fr"WHERE name = $name")
      .query[DatabaseGroup]
      .option
      .transact(transactor)
  }

  override def findByUser(user: DatabaseUser): F[List[DatabaseGroup]] = {
    (selectGroup ++ fr"INNER JOIN users_to_groups"
      ++ fr"ON user_groups.id = users_to_groups.group_id"
      ++ fr"WHERE users_to_groups.user_id = ${user.id}")
      .query[DatabaseGroup]
      .to[List]
      .transact(transactor)
  }

  override def findByItem(item: DatabaseItem): F[List[DatabaseGroup]] = {
    (selectGroup ++ fr"INNER JOIN items_to_groups"
      ++ fr"ON user_groups.id = items_to_groups.group_id"
      ++ fr"WHERE items_to_groups.item_id = ${item.id}")
      .query[DatabaseGroup]
      .to[List]
      .transact(transactor)
  }

  override def create(group: DatabaseGroup): F[GroupId] = {
    (insertGroup ++ fr"VALUES (${group.name})").update
      .withUniqueGeneratedKeys[GroupId]("id")
      .transact(transactor)
  }

  override def update(group: DatabaseGroup): F[Int] = {
    (setGroup ++ fr"SET name = ${group.name} WHERE id = ${group.id}").update.run
      .transact(transactor)
  }

  override def delete(id: GroupId): F[Int] = {
    (deleteGroup ++ fr"WHERE id = $id").update.run
      .transact(transactor)
  }

  override def existsUserInGroup(group: DatabaseGroup, user: DatabaseUser): F[Boolean] = {
    val selected =
      (exists ++ fr"(SELECT id FROM users_to_groups WHERE group_id = ${group.id} AND user_id = ${user.id})")
        .query[Int]
        .unique
        .transact(transactor)

    for {
      value <- selected
    } yield value == 1
  }

  override def addUserToGroup(group: DatabaseGroup, user: DatabaseUser): F[Int] = {
    (insertUserToGroup ++ fr"VALUES (${user.id}, ${group.id})").update.run
      .transact(transactor)
  }

  override def removeUserFromGroup(group: DatabaseGroup, user: DatabaseUser): F[Int] = {
    (deleteUserFromGroup ++ fr"WHERE user_id = ${user.id} AND group_id = ${group.id}").update.run
      .transact(transactor)
  }

  override def existsItemInGroup(group: DatabaseGroup, item: DatabaseItem): F[Boolean] = {
    val selected =
      (exists ++ fr"(SELECT id FROM items_to_groups WHERE group_id = ${group.id} AND item_id = ${item.id})")
        .query[Int]
        .unique
        .transact(transactor)

    for {
      value <- selected
    } yield value == 1
  }

  override def addItemToGroup(group: DatabaseGroup, item: DatabaseItem): F[Int] = {
    (insertItemToGroup ++ fr"VALUES (${item.id}, ${group.id})").update.run
      .transact(transactor)
  }

  override def removeItemFromGroup(group: DatabaseGroup, item: DatabaseItem): F[Int] = {
    (deleteItemFromGroup ++ fr"WHERE item_id = ${item.id} AND group_id = ${group.id}").update.run
      .transact(transactor)
  }
}
