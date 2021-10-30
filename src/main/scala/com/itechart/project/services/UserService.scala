package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.authentication.Crypto
import com.itechart.project.dto.user.{FullUserDto, UserDto}
import com.itechart.project.repository.{CategoryRepository, SupplierRepository, UserRepository}
import com.itechart.project.services.error.UserErrors.UserValidationError
import com.itechart.project.services.impl.UserServiceImpl
import io.chrisdavenport.log4cats.Logger

trait UserService[F[_]] {
  def findAllUsers: F[List[FullUserDto]]
  def findById(id:                Long):            F[Either[UserValidationError, FullUserDto]]
  def createUser(user:            UserDto): F[Either[UserValidationError, FullUserDto]]
  def updateUser(id:              Long, user: UserDto): F[Either[UserValidationError, Boolean]]
  def subscribeCategory(userId:   Long, categoryId: Long): F[Either[UserValidationError, Boolean]]
  def unsubscribeCategory(userId: Long, categoryId: Long): F[Either[UserValidationError, Boolean]]
  def subscribeSupplier(userId:   Long, supplierId: Long): F[Either[UserValidationError, Boolean]]
  def unsubscribeSupplier(userId: Long, supplierId: Long): F[Either[UserValidationError, Boolean]]
}

object UserService {
  def of[F[_]: Sync: Logger](
    userRepository:     UserRepository[F],
    supplierRepository: SupplierRepository[F],
    categoryRepository: CategoryRepository[F],
    crypto:             Crypto
  ): UserService[F] =
    new UserServiceImpl[F](
      userRepository,
      supplierRepository,
      categoryRepository,
      crypto
    )
}
