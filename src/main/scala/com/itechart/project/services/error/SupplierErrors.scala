package com.itechart.project.services.error

import scala.util.control.NoStackTrace

object SupplierErrors {

  sealed trait SupplierValidationError extends RuntimeException with NoStackTrace {
    def message: String
  }

  object SupplierValidationError {
    // 404
    final case class SupplierNotFound(supplierId: Long) extends SupplierValidationError {
      override def message: String =
        s"The supplier with id `$supplierId` is not found"
    }

    // 400
    final case object InvalidSupplierName extends SupplierValidationError {
      override def message: String = "Supplier name is empty"
    }

    // 409
    final case class SupplierAlreadyExists(name: String) extends SupplierValidationError {
      override def message: String =
        s"The supplier with name `$name` already exists"
    }
  }

}
