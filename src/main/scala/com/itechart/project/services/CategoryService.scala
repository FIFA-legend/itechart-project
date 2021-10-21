package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.repository.CategoryRepository
import com.itechart.project.services.error.CategoryErrors.CategoryValidationError
import com.itechart.project.services.impl.CategoryServiceImpl
import io.chrisdavenport.log4cats.Logger

trait CategoryService[F[_]] {
  def findAllCategories: F[List[CategoryDto]]
  def findById(id:             Long):        F[Either[CategoryValidationError, CategoryDto]]
  def createCategory(category: CategoryDto): F[Either[CategoryValidationError, CategoryDto]]
  def updateCategory(category: CategoryDto): F[Either[CategoryValidationError, CategoryDto]]
  def deleteCategory(id:       Long):        F[Either[CategoryValidationError, Boolean]]
}

object CategoryService {
  def of[F[_]: Sync: Logger](categoryRepository: CategoryRepository[F]): CategoryService[F] =
    new CategoryServiceImpl[F](categoryRepository)
}
