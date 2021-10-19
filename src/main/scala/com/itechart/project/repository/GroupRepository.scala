package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.group.{DatabaseGroup, GroupId}
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.impl.DoobieGroupRepository
import doobie.Transactor

trait GroupRepository[F[_]] {
  def all: F[List[DatabaseGroup]]
  def findById(id:     GroupId):       F[Option[DatabaseGroup]]
  def findByUser(user: DatabaseUser):  F[List[DatabaseGroup]]
  def findByItem(item: DatabaseItem):  F[List[DatabaseGroup]]
  def create(group:    DatabaseGroup): F[GroupId]
  def update(group:    DatabaseGroup): F[Int]
  def delete(id:       GroupId):       F[Int]
}

object GroupRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieGroupRepository[F] =
    new DoobieGroupRepository[F](transactor)
}
