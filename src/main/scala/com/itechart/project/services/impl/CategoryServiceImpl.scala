package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.category.{CategoryId, CategoryName, DatabaseCategory}
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.repository.CategoryRepository
import com.itechart.project.services.CategoryService
import com.itechart.project.services.error.CategoryErrors.CategoryValidationError
import com.itechart.project.services.error.CategoryErrors.CategoryValidationError.{
  CategoryAlreadyExists,
  CategoryNotFound,
  InvalidCategoryName
}
import com.itechart.project.util.ModelMapper._
import com.itechart.project.util.RefinedConversion._
import eu.timepit.refined.collection.NonEmpty
import io.chrisdavenport.log4cats.Logger

class CategoryServiceImpl[F[_]: Sync: Logger](categoryRepository: CategoryRepository[F]) extends CategoryService[F] {
  override def findAllCategories: F[List[CategoryDto]] = {
    Logger[F].info(s"Selecting all categories from database")
    for {
      categories <- categoryRepository.all
      category   <- categories.traverse(c => categoryDomainToDto(c).pure[F])
    } yield category
  }

  override def findById(id: Long): F[Either[CategoryValidationError, CategoryDto]] = {
    Logger[F].info(s"Selecting category with id = $id from database")
    val result: EitherT[F, CategoryValidationError, CategoryDto] = for {
      category <- EitherT.fromOptionF[F, CategoryValidationError, DatabaseCategory](
        categoryRepository.findById(CategoryId(id)),
        CategoryNotFound(id)
      )
      dto = categoryDomainToDto(category)
    } yield dto

    result.value
  }

  override def createCategory(category: CategoryDto): F[Either[CategoryValidationError, CategoryDto]] = {
    Logger[F].info(s"Creating new category in database")
    val result: EitherT[F, CategoryValidationError, CategoryDto] = for {
      _     <- EitherT(validateCategory(category).pure[F])
      domain = categoryDtoToDomain(category)
      _     <- EitherT(existsByName(domain))

      id <- EitherT.liftF(categoryRepository.create(domain))
    } yield categoryDomainToDto(domain.copy(id = id))

    result.value
  }

  override def updateCategory(category: CategoryDto): F[Either[CategoryValidationError, CategoryDto]] = {
    Logger[F].info(s"Updating category with id = ${category.id} in database")
    val result: EitherT[F, CategoryValidationError, CategoryDto] = for {
      _     <- EitherT.fromOptionF(categoryRepository.findById(CategoryId(category.id)), CategoryNotFound(category.id))
      _     <- EitherT(validateCategory(category).pure[F])
      domain = categoryDtoToDomain(category)
      _     <- EitherT(existsByName(domain))

      _ <- EitherT.liftF(categoryRepository.update(domain))
    } yield categoryDomainToDto(domain)

    result.value
  }

  override def deleteCategory(id: Long): F[Either[CategoryValidationError, Boolean]] = {
    Logger[F].info(s"Deleting category with id = $id from database")
    val result: EitherT[F, CategoryValidationError, Boolean] = for {
      _       <- EitherT.fromOptionF(categoryRepository.findById(CategoryId(id)), CategoryNotFound(id))
      deleted <- EitherT.liftF(categoryRepository.delete(CategoryId(id)))
    } yield deleted != 0

    result.value
  }

  private def existsByName(category: DatabaseCategory): F[Either[CategoryValidationError, DatabaseCategory]] = {
    categoryRepository.findByName(category.name).map {
      case None    => category.asRight[CategoryValidationError]
      case Some(_) => CategoryAlreadyExists(category.name.value).asLeft[DatabaseCategory]
    }
  }

  private def validateCategory(category: CategoryDto): Either[CategoryValidationError, CategoryDto] = {
    for {
      _ <- validateName(category.name)
    } yield category
  }

  private def validateName(name: String): Either[CategoryValidationError, CategoryName] = {
    validateParameter[CategoryValidationError, String, NonEmpty](name, InvalidCategoryName)
  }

}
