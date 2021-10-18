package com.itechart.project.domain

import eu.timepit.refined.types.string.NonEmptyString

object category {

  final case class CategoryId(id: Long)

  type CategoryName = NonEmptyString

  final case class DatabaseCategory(id: CategoryId, name: CategoryName)

}
