package com.itechart.project.domain

import eu.timepit.refined.types.string.NonEmptyString

object group {

  final case class GroupId(id: Long)

  type GroupName = NonEmptyString

  final case class DatabaseGroup(id: GroupId, name: GroupName)

}
