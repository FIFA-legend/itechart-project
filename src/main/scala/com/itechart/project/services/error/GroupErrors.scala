package com.itechart.project.services.error

import com.itechart.project.domain.group.GroupName

import scala.util.control.NoStackTrace

object GroupErrors {

  sealed trait GroupValidationError extends RuntimeException with NoStackTrace {
    def message: String
  }

  object GroupValidationError {
    // 404
    final case class GroupNotFound(groupId: Long) extends GroupValidationError {
      override def message: String = s"The group with id `$groupId` is not found"
    }

    // 400
    final case object InvalidGroupName extends GroupValidationError {
      override def message: String = "Group name is empty"
    }

    // 409
    final case class GroupNameInUse(name: GroupName) extends GroupValidationError {
      override def message: String = s"Group name `${name.value}` is taken already"
    }

    // 404
    final case class UserNotFound(userId: Long) extends GroupValidationError {
      override def message: String = s"The group with id `$userId` is not found"
    }

    // 404
    final case class ItemNotFound(itemId: Long) extends GroupValidationError {
      override def message: String = s"The group with id `$itemId` is not found"
    }

    // 409
    final case class UserIsInGroup(userId: Long, groupId: Long) extends GroupValidationError {
      override def message: String = s"The user with id `$userId` is in group `$groupId` already"
    }

    // 409
    final case class ItemIsInGroup(itemId: Long, groupId: Long) extends GroupValidationError {
      override def message: String = s"The item with id `$itemId` is in group `$groupId` already"
    }
  }

}
