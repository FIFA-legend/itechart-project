package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.dto.group.GroupDto
import com.itechart.project.repository.{GroupRepository, ItemRepository, UserRepository}
import com.itechart.project.services.error.GroupValidationErrors.GroupValidationError
import com.itechart.project.services.impl.GroupServiceImpl
import io.chrisdavenport.log4cats.Logger

trait GroupService[F[_]] {
  def findAllGroups: F[List[GroupDto]]
  def findAllByUser(userId:        Long):        F[Either[GroupValidationError, List[GroupDto]]]
  def findAllByItem(itemId:        Long):        F[Either[GroupValidationError, List[GroupDto]]]
  def findById(id:                 Long):        F[Either[GroupValidationError, GroupDto]]
  def createGroup(group:           GroupDto):    F[Either[GroupValidationError, GroupDto]]
  def updateGroup(group:           GroupDto):    F[Either[GroupValidationError, GroupDto]]
  def deleteGroup(id:              Long): F[Either[GroupValidationError, Boolean]]
  def addUserToGroup(groupId:      Long, userId: Long): F[Either[GroupValidationError, Boolean]]
  def removeUserFromGroup(groupId: Long, userId: Long): F[Either[GroupValidationError, Boolean]]
  def addItemToGroup(groupId:      Long, itemId: Long): F[Either[GroupValidationError, Boolean]]
  def removeItemFromGroup(groupId: Long, itemId: Long): F[Either[GroupValidationError, Boolean]]
}

object GroupService {
  def of[F[_]: Sync: Logger](
    groupRepository: GroupRepository[F],
    userRepository:  UserRepository[F],
    itemRepository:  ItemRepository[F]
  ): GroupService[F] =
    new GroupServiceImpl[F](groupRepository, userRepository, itemRepository)
}
