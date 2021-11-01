package com.itechart.project.domain

import eu.timepit.refined.types.string.NonEmptyString

object supplier {

  final case class SupplierId(value: Long)

  type SupplierName = NonEmptyString

  final case class DatabaseSupplier(id: SupplierId, name: SupplierName)

}
