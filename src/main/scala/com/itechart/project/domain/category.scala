package com.itechart.project.domain

import eu.timepit.refined.types.string.NonEmptyString

object category {

  type CategoryName = NonEmptyString

  final case class Category(
    id:   Long,
    name: CategoryName
  )

}
