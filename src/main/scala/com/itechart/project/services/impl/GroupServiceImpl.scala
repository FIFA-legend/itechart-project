package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.group.{DatabaseGroup, GroupId}
import com.itechart.project.domain.item.{DatabaseItem, ItemId}
import com.itechart.project.domain.user.{DatabaseUser, UserId}
import com.itechart.project.dto.group.GroupDto
import com.itechart.project.repository.{GroupRepository, ItemRepository, UserRepository}
import com.itechart.project.services.GroupService
import com.itechart.project.services.error.GroupValidationErrors.GroupValidationError
import com.itechart.project.services.error.GroupValidationErrors.GroupValidationError.{
  GroupNameInUse,
  GroupNotFound,
  InvalidGroupName,
  ItemIsInGroup,
  ItemNotFound,
  UserIsInGroup,
  UserNotFound
}
import com.itechart.project.util.ModelMapper.{groupDomainToDto, itemDomainToSimpleItemDto, userDomainToSimpleUserDto}
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.collection.NonEmpty
import io.chrisdavenport.log4cats.Logger

class GroupServiceImpl[F[_]: Sync: Logger](
  groupRepository: GroupRepository[F],
  userRepository:  UserRepository[F],
  itemRepository:  ItemRepository[F]
) extends GroupService[F] {
  override def findAllGroups: F[List[GroupDto]] = {
    for {
      groups    <- groupRepository.all
      dtoGroups <- groups.map(fulfillGroup).sequence
    } yield dtoGroups
  }

  override def findAllByUser(userId: Long): F[Either[GroupValidationError, List[GroupDto]]] = {
    val result: EitherT[F, GroupValidationError, List[GroupDto]] = for {
      domainUser <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))
      userGroups <- EitherT.liftF(groupRepository.findByUser(domainUser))

      dtoUserGroups <- EitherT.liftF(userGroups.map(fulfillGroup).sequence)
    } yield dtoUserGroups

    result.value
  }

  override def findAllByItem(itemId: Long): F[Either[GroupValidationError, List[GroupDto]]] = {
    val result: EitherT[F, GroupValidationError, List[GroupDto]] = for {
      domainItem <- EitherT.fromOptionF(itemRepository.findById(ItemId(itemId)), ItemNotFound(itemId))
      itemGroups <- EitherT.liftF(groupRepository.findByItem(domainItem))

      dtoItemGroups <- EitherT.liftF(itemGroups.map(fulfillGroup).sequence)
    } yield dtoItemGroups

    result.value
  }

  override def findById(id: Long): F[Either[GroupValidationError, GroupDto]] = {
    val result: EitherT[F, GroupValidationError, GroupDto] = for {
      domainGroup <- EitherT.fromOptionF(groupRepository.findById(GroupId(id)), GroupNotFound(id))
      dtoGroup    <- EitherT.liftF(fulfillGroup(domainGroup))
    } yield dtoGroup

    result.value
  }

  override def createGroup(group: GroupDto): F[Either[GroupValidationError, GroupDto]] = {
    val result: EitherT[F, GroupValidationError, GroupDto] = for {
      domainGroup <- EitherT(validateGroup(group))

      id       <- EitherT.liftF(groupRepository.create(domainGroup))
      dtoGroup <- EitherT.liftF(fulfillGroup(domainGroup.copy(id = id)))
    } yield dtoGroup

    result.value
  }

  override def updateGroup(group: GroupDto): F[Either[GroupValidationError, GroupDto]] = {
    val result: EitherT[F, GroupValidationError, GroupDto] = for {
      previousDomainGroup <- EitherT.fromOptionF(groupRepository.findById(GroupId(group.id)), GroupNotFound(group.id))
      newDomainGroup      <- EitherT(validateGroup(group))

      domainGroupWithId = newDomainGroup.copy(id = previousDomainGroup.id)

      _        <- EitherT.liftF(groupRepository.update(domainGroupWithId))
      dtoGroup <- EitherT.liftF(fulfillGroup(domainGroupWithId))
    } yield dtoGroup

    result.value
  }

  override def deleteGroup(id: Long): F[Either[GroupValidationError, Boolean]] = {
    val result: EitherT[F, GroupValidationError, Boolean] = for {
      groupDomain <- EitherT.fromOptionF(groupRepository.findById(GroupId(id)), GroupNotFound(id))
      usersDomain <- EitherT.liftF(userRepository.findByGroup(groupDomain))
      itemsDomain <- EitherT.liftF(itemRepository.findByGroup(groupDomain))

      _ <- EitherT.liftF(usersDomain.map(user => groupRepository.removeUserFromGroup(groupDomain, user)).sequence)
      _ <- EitherT.liftF(itemsDomain.map(item => groupRepository.removeItemFromGroup(groupDomain, item)).sequence)

      deleted <- EitherT.liftF(groupRepository.delete(groupDomain.id))
    } yield deleted != 0

    result.value
  }

  override def addUserToGroup(groupId: Long, userId: Long): F[Either[GroupValidationError, Boolean]] = {
    val result: EitherT[F, GroupValidationError, Boolean] = for {
      groupDomain <- EitherT.fromOptionF(groupRepository.findById(GroupId(groupId)), GroupNotFound(groupId))
      userDomain  <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))
      _           <- EitherT(validateUserInGroup(groupDomain, userDomain))

      updated <- EitherT.liftF(groupRepository.addUserToGroup(groupDomain, userDomain))
    } yield updated != 0

    result.value
  }

  override def removeUserFromGroup(groupId: Long, userId: Long): F[Either[GroupValidationError, Boolean]] = {
    val result: EitherT[F, GroupValidationError, Boolean] = for {
      groupDomain <- EitherT.fromOptionF(groupRepository.findById(GroupId(groupId)), GroupNotFound(groupId))
      userDomain  <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))

      updated <- EitherT.liftF(groupRepository.removeUserFromGroup(groupDomain, userDomain))
    } yield updated != 0

    result.value
  }

  override def addItemToGroup(groupId: Long, itemId: Long): F[Either[GroupValidationError, Boolean]] = {
    val result: EitherT[F, GroupValidationError, Boolean] = for {
      groupDomain <- EitherT.fromOptionF(groupRepository.findById(GroupId(groupId)), GroupNotFound(groupId))
      itemDomain  <- EitherT.fromOptionF(itemRepository.findById(ItemId(itemId)), ItemNotFound(itemId))
      _           <- EitherT(validateItemInGroup(groupDomain, itemDomain))

      updated <- EitherT.liftF(groupRepository.addItemToGroup(groupDomain, itemDomain))
    } yield updated != 0

    result.value
  }

  override def removeItemFromGroup(groupId: Long, itemId: Long): F[Either[GroupValidationError, Boolean]] = {
    val result: EitherT[F, GroupValidationError, Boolean] = for {
      groupDomain <- EitherT.fromOptionF(groupRepository.findById(GroupId(groupId)), GroupNotFound(groupId))
      itemDomain  <- EitherT.fromOptionF(itemRepository.findById(ItemId(itemId)), ItemNotFound(itemId))

      updated <- EitherT.liftF(groupRepository.removeItemFromGroup(groupDomain, itemDomain))
    } yield updated != 0

    result.value
  }

  private def validateGroup(group: GroupDto): F[Either[GroupValidationError, DatabaseGroup]] = {
    val result: EitherT[F, GroupValidationError, DatabaseGroup] = for {
      name <- EitherT(validateParameter[GroupValidationError, String, NonEmpty](group.name, InvalidGroupName).pure[F])

      option <- EitherT.liftF(groupRepository.findByName(name))
      either = option match {
        case Some(_) => GroupNameInUse(name).asLeft[DatabaseGroup]
        case None    => DatabaseGroup(GroupId(0), name).asRight[GroupValidationError]
      }
      result <- EitherT(either.pure[F])
    } yield result

    result.value
  }

  private def validateUserInGroup(
    group: DatabaseGroup,
    user:  DatabaseUser
  ): F[Either[GroupValidationError, Boolean]] = {
    for {
      isUserInGroup <- groupRepository.existsUserInGroup(group, user)

      either =
        if (isUserInGroup) {
          UserIsInGroup(user.id.value, group.id.value).asLeft[Boolean]
        } else {
          true.asRight[GroupValidationError]
        }
    } yield either
  }

  private def validateItemInGroup(
    group: DatabaseGroup,
    item:  DatabaseItem
  ): F[Either[GroupValidationError, Boolean]] = {
    for {
      isItemInGroup <- groupRepository.existsItemInGroup(group, item)

      either =
        if (isItemInGroup) {
          ItemIsInGroup(item.id.value, group.id.value).asLeft[Boolean]
        } else {
          true.asRight[GroupValidationError]
        }
    } yield either
  }

  private def fulfillGroup(group: DatabaseGroup): F[GroupDto] = {
    for {
      items <- itemRepository.findByGroup(group)
      users <- userRepository.findByGroup(group)

      dtoItems = items.map(itemDomainToSimpleItemDto)
      dtoUsers = users.map(userDomainToSimpleUserDto)
    } yield groupDomainToDto(group, dtoUsers, dtoItems)
  }
}
