package com.itechart.project.services.error

import scala.util.control.NoStackTrace

object CategoryErrors {

  sealed trait CategoryValidationError extends RuntimeException with NoStackTrace {
    def message: String
  }

  object CategoryValidationError {
    // 404
    final case class CategoryNotFound(categoryId: Long) extends CategoryValidationError {
      override def message: String = s"The category with id `$categoryId` is not found"
    }

    // 400
    final case object InvalidCategoryName extends CategoryValidationError {
      override def message: String = "Category name is empty"
    }

    // 409
    final case class CategoryInUse(name: String) extends CategoryValidationError {
      override def message: String = s"The category with name `$name` already exists"
    }

    // 409
    final case class CategoryIsConnected(categoryId: Long) extends CategoryValidationError {
      override def message: String = s"The category with id `$categoryId` is part of other entities"
    }

    // 400
    final case class UnsupportedCategoryError(msg: String) extends CategoryValidationError {
      override def message: String = msg
    }
  }

}
