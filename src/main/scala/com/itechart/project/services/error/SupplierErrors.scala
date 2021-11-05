package com.itechart.project.services.error

object SupplierErrors {

  sealed trait SupplierValidationError extends ValidationError

  object SupplierValidationError {
    // 404
    final case class SupplierNotFound(supplierId: Long) extends SupplierValidationError {
      override def message: String = s"The supplier with id `$supplierId` is not found"
    }

    // 400
    final case object InvalidSupplierName extends SupplierValidationError {
      override def message: String = s"Supplier name is empty"
    }

    // 409
    final case class SupplierInUse(name: String) extends SupplierValidationError {
      override def message: String = s"The supplier with name `$name` already exists"
    }

    // 409
    final case class SupplierIsConnected(supplierId: Long) extends SupplierValidationError {
      override def message: String = s"The supplier with id `$supplierId` is part of other entities"
    }

    // 400
    final case class UnsupportedSupplierError(msg: String) extends SupplierValidationError {
      override def message: String = msg
    }
  }

}
