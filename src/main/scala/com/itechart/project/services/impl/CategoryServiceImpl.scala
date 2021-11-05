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
  CategoryInUse,
  CategoryIsConnected,
  CategoryNotFound,
  InvalidCategoryName,
  UnsupportedCategoryError
}
import com.itechart.project.util.ModelMapper._
import com.itechart.project.util.RefinedConversion._
import eu.timepit.refined.collection.NonEmpty
import org.typelevel.log4cats.Logger

import java.sql.SQLIntegrityConstraintViolationException

class CategoryServiceImpl[F[_]: Sync: Logger](categoryRepository: CategoryRepository[F]) extends CategoryService[F] {
  override def findAllCategories: F[List[CategoryDto]] = {
    for {
      _          <- Logger[F].info(s"Selecting all categories from database")
      categories <- categoryRepository.all
      category   <- categories.traverse(c => categoryDomainToDto(c).pure[F])
      _          <- Logger[F].info(s"Selected ${categories.size} categories from database")
    } yield category
  }

  override def findById(id: Long): F[Either[CategoryValidationError, CategoryDto]] = {
    val result: EitherT[F, CategoryValidationError, CategoryDto] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Selecting category with id = $id from database"))
      category   <- EitherT.fromOptionF(categoryRepository.findById(CategoryId(id)), CategoryNotFound(id))
      dtoCategory = categoryDomainToDto(category)
      _          <- EitherT.liftF(Logger[F].info(s"Category with id = $id selected successfully"))
    } yield dtoCategory

    result.value
  }

  override def createCategory(category: CategoryDto): F[Either[CategoryValidationError, CategoryDto]] = {
    val result: EitherT[F, CategoryValidationError, CategoryDto] = for {
      _             <- EitherT.liftF(Logger[F].info(s"Creating new category in database"))
      _             <- EitherT(validateCategory(category).pure[F])
      domainCategory = categoryDtoToDomain(category)
      _             <- EitherT(existsByName(domainCategory))

      id <- EitherT.liftF(categoryRepository.create(domainCategory))
      _  <- EitherT.liftF(Logger[F].info(s"New category created successfully. It's id = $id"))
    } yield categoryDomainToDto(domainCategory.copy(id = id))

    result.value
  }

  override def updateCategory(category: CategoryDto): F[Either[CategoryValidationError, CategoryDto]] = {
    val result: EitherT[F, CategoryValidationError, CategoryDto] = for {
      _ <- EitherT.liftF(Logger[F].info(s"Updating category with id = ${category.id} in database"))
      _ <- EitherT.fromOptionF(categoryRepository.findById(CategoryId(category.id)), CategoryNotFound(category.id))
      _ <- EitherT(validateCategory(category).pure[F])

      domainCategory = categoryDtoToDomain(category)
      _             <- EitherT(existsByName(domainCategory))

      updated <- EitherT.liftF(categoryRepository.update(domainCategory))
      _       <- EitherT.liftF(Logger[F].info(s"Category with id = ${category.id} update status: ${updated != 0}"))
    } yield categoryDomainToDto(domainCategory)

    result.value
  }

  override def deleteCategory(id: Long): F[Either[CategoryValidationError, Boolean]] = {
    val result: EitherT[F, CategoryValidationError, Boolean] = for {
      _ <- EitherT.liftF(Logger[F].info(s"Deleting category with id = $id from database"))
      _ <- EitherT.fromOptionF(categoryRepository.findById(CategoryId(id)), CategoryNotFound(id))
      deleted <- EitherT(categoryRepository.delete(CategoryId(id)).attempt).leftMap {
        case _: SQLIntegrityConstraintViolationException => CategoryIsConnected(id)
        case error => UnsupportedCategoryError(error.getMessage)
      }
      _ <- EitherT.liftF(Logger[F].info(s"Category with id = $id delete status: ${deleted != 0}"))
    } yield deleted != 0

    result.value
  }

  private def existsByName(category: DatabaseCategory): F[Either[CategoryValidationError, DatabaseCategory]] = {
    categoryRepository.findByName(category.name).map {
      case None    => category.asRight[CategoryValidationError]
      case Some(_) => CategoryInUse(category.name.value).asLeft[DatabaseCategory]
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
