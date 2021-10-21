package com.itechart.project.services.error

import com.itechart.project.dto.category.CategoryDto

import scala.util.control.NoStackTrace

object CategoryErrors {

  sealed trait CategoryValidationError extends RuntimeException with NoStackTrace {
    def message: String
  }

  object CategoryValidationError {
    // 404
    final case class CategoryNotFound(categoryId: Long) extends CategoryValidationError {
      override def message: String =
        s"The category with id `$categoryId` is not found"
    }

    // 400
    final case object InvalidCategoryName extends CategoryValidationError {
      override def message: String = "Category name is empty"
    }

    // 409
    final case class CategoryAlreadyExists(name: String) extends CategoryValidationError {
      override def message: String =
        s"The category with name `$name` already exists"
    }
  }

}
