package com.itechart.project.repository

import cats.effect.Sync
import com.itechart.project.domain.category.{CategoryId, CategoryName, DatabaseCategory}
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.repository.impl.DoobieCategoryRepository
import doobie.util.transactor.Transactor

trait CategoryRepository[F[_]] {
  def all: F[List[DatabaseCategory]]
  def findById(id:            CategoryId):       F[Option[DatabaseCategory]]
  def findByName(name:        CategoryName):     F[Option[DatabaseCategory]]
  def findByUser(user:        DatabaseUser):     F[List[DatabaseCategory]]
  def findByItem(item:        DatabaseItem):     F[List[DatabaseCategory]]
  def create(category:        DatabaseCategory): F[CategoryId]
  def update(category:        DatabaseCategory): F[Int]
  def delete(id:              CategoryId): F[Int]
  def createLinksToItem(item: DatabaseItem, categories: List[DatabaseCategory]): F[Int]
  def updateLinksToItem(item: DatabaseItem, categories: List[DatabaseCategory]): F[Int]
  def deleteLinksToItem(item: DatabaseItem): F[Int]
}

object CategoryRepository {
  def of[F[_]: Sync](transactor: Transactor[F]): DoobieCategoryRepository[F] =
    new DoobieCategoryRepository[F](transactor)
}
