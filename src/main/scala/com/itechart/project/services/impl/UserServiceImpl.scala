package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.authentication.Crypto
import com.itechart.project.domain.category.{CategoryId, DatabaseCategory}
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId}
import com.itechart.project.domain.user.{DatabaseUser, Email, EncryptedPassword, Password, UserId, Username}
import com.itechart.project.dto.user.{FullUserDto, UserDto}
import com.itechart.project.repository.{CategoryRepository, SupplierRepository, UserRepository}
import com.itechart.project.services.UserService
import com.itechart.project.services.error.UserErrors.UserValidationError
import com.itechart.project.services.error.UserErrors.UserValidationError.{
  CategoryIsSubscribed,
  CategoryNotFound,
  EmailInUse,
  InvalidEmail,
  InvalidPassword,
  InvalidUsernameLength,
  InvalidUsernameSymbols,
  SupplierIsSubscribed,
  SupplierNotFound,
  UserNotFound,
  UsernameInUse
}
import com.itechart.project.util.ModelMapper.{categoryDomainToDto, supplierDomainToDto, userDomainToFullUserDto}
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex
import io.chrisdavenport.log4cats.Logger

class UserServiceImpl[F[_]: Sync: Logger](
  userRepository:     UserRepository[F],
  supplierRepository: SupplierRepository[F],
  categoryRepository: CategoryRepository[F],
  crypto:             Crypto
) extends UserService[F] {
  override def findAllUsers: F[List[FullUserDto]] = {
    for {
      _           <- Logger[F].info(s"Selecting all users from database")
      domainUsers <- userRepository.all
      dtoUsers    <- domainUsers.map(fulfillUser).sequence
      _           <- Logger[F].info(s"Selected ${domainUsers.size} users from database")
    } yield dtoUsers
  }

  override def findById(id: Long): F[Either[UserValidationError, FullUserDto]] = {
    val res: EitherT[F, UserValidationError, FullUserDto] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Selecting user with id = $id from database"))
      userDomain <- EitherT.fromOptionF(userRepository.findById(UserId(id)), UserNotFound(id))
      dtoUser    <- EitherT.liftF(fulfillUser(userDomain))
      _          <- EitherT.liftF(Logger[F].info(s"User with id = $id selected successfully"))
    } yield dtoUser

    res.value
  }

  override def createUser(user: UserDto): F[Either[UserValidationError, FullUserDto]] = {
    val result: EitherT[F, UserValidationError, FullUserDto] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Creating new user in database"))
      domainUser <- EitherT(validateUser(user, UserId(0)))

      id      <- EitherT.liftF(userRepository.create(domainUser))
      dtoUser <- EitherT.liftF(fulfillUser(domainUser.copy(id = id)))
      _       <- EitherT.liftF(Logger[F].info(s"New user created successfully. It's id = $id "))
    } yield dtoUser

    result.value
  }

  override def updateUser(id: Long, user: UserDto): F[Either[UserValidationError, Boolean]] = {
    val result: EitherT[F, UserValidationError, Boolean] = for {
      _             <- EitherT.liftF(Logger[F].info(s"Updating user with id = $id in database"))
      foundUser     <- EitherT.fromOptionF(userRepository.findById(UserId(id)), UserNotFound(id))
      newDomainUser <- EitherT(validateUser(user, foundUser.id))

      updated <- EitherT.liftF(userRepository.update(newDomainUser.copy(id = foundUser.id)))
      _       <- EitherT.liftF(Logger[F].info(s"User with id = $id update status: ${updated != 0}"))
    } yield updated != 0

    result.value
  }

  override def subscribeCategory(userId: Long, categoryId: Long): F[Either[UserValidationError, Boolean]] = {
    val result: EitherT[F, UserValidationError, Boolean] = for {
      _ <- EitherT.liftF(
        Logger[F].info(s"User with id = $userId subscription on category with id = $categoryId started")
      )
      userDomain <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))
      categoryDomain <- EitherT.fromOptionF(
        categoryRepository.findById(CategoryId(categoryId)),
        CategoryNotFound(categoryId)
      )
      _ <- EitherT(validateUserSubscriptionOnCategory(userDomain, categoryDomain))

      updated <- EitherT.liftF(userRepository.subscribeToCategory(userDomain, categoryDomain))
      _ <- EitherT.liftF(
        Logger[F].info(s"User with id = $userId subscription on category with id = $categoryId completed successfully")
      )
    } yield updated != 0

    result.value
  }

  override def unsubscribeCategory(userId: Long, categoryId: Long): F[Either[UserValidationError, Boolean]] = {
    val result: EitherT[F, UserValidationError, Boolean] = for {
      _ <- EitherT.liftF(
        Logger[F].info(s"User with id = $userId unsubscription from category with id = $categoryId started")
      )
      userDomain <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))
      categoryDomain <- EitherT.fromOptionF(
        categoryRepository.findById(CategoryId(categoryId)),
        CategoryNotFound(categoryId)
      )

      updated <- EitherT.liftF(userRepository.unsubscribeFromCategory(userDomain, categoryDomain))
      _ <- EitherT.liftF(
        Logger[F].info(
          s"User with id = $userId unsubscription from category with id = $categoryId completed successfully"
        )
      )
    } yield updated != 0

    result.value
  }

  override def subscribeSupplier(userId: Long, supplierId: Long): F[Either[UserValidationError, Boolean]] = {
    val result: EitherT[F, UserValidationError, Boolean] = for {
      _ <- EitherT.liftF(
        Logger[F].info(s"User with id = $userId subscription on supplier with id = $supplierId started")
      )
      userDomain <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))
      supplierDomain <- EitherT.fromOptionF(
        supplierRepository.findById(SupplierId(supplierId)),
        SupplierNotFound(supplierId)
      )
      _ <- EitherT(validateUserSubscriptionOnSupplier(userDomain, supplierDomain))

      updated <- EitherT.liftF(userRepository.subscribeToSupplier(userDomain, supplierDomain))
      _ <- EitherT.liftF(
        Logger[F].info(s"User with id = $userId subscription on supplier with id = $supplierId completed successfully")
      )
    } yield updated != 0

    result.value
  }

  override def unsubscribeSupplier(userId: Long, supplierId: Long): F[Either[UserValidationError, Boolean]] = {
    val result: EitherT[F, UserValidationError, Boolean] = for {
      _ <- EitherT.liftF(
        Logger[F].info(s"User with id = $userId unsubscription from supplier with id = $supplierId started")
      )
      userDomain <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), UserNotFound(userId))
      supplierDomain <- EitherT.fromOptionF(
        supplierRepository.findById(SupplierId(supplierId)),
        SupplierNotFound(supplierId)
      )

      updated <- EitherT.liftF(userRepository.unsubscribeFromSupplier(userDomain, supplierDomain))
      _ <- EitherT.liftF(
        Logger[F].info(
          s"User with id = $userId unsubscription from supplier with id = $supplierId completed successfully"
        )
      )
    } yield updated != 0

    result.value
  }

  private def validateUserSubscriptionOnCategory(
    user:     DatabaseUser,
    category: DatabaseCategory
  ): F[Either[UserValidationError, Boolean]] = {
    for {
      isUserSubscribed <- userRepository.isUserSubscribedOnCategory(user, category)

      either =
        if (isUserSubscribed) {
          CategoryIsSubscribed(user.id.value, category.id.value).asLeft[Boolean]
        } else {
          true.asRight[UserValidationError]
        }
    } yield either
  }

  private def validateUserSubscriptionOnSupplier(
    user:     DatabaseUser,
    supplier: DatabaseSupplier
  ): F[Either[UserValidationError, Boolean]] = {
    for {
      isUserSubscribed <- userRepository.isUserSubscribedOnSupplier(user, supplier)

      either =
        if (isUserSubscribed) {
          SupplierIsSubscribed(user.id.value, supplier.id.value).asLeft[Boolean]
        } else {
          true.asRight[UserValidationError]
        }
    } yield either
  }

  private def validateUser(user: UserDto, userId: UserId): F[Either[UserValidationError, DatabaseUser]] = {
    for {
      eitherUsername <- validateUsername(user.username, userId)
      eitherPassword <- validatePassword(user.password)
      eitherEmail    <- validateEmail(user.email, userId)

      domainUser <- (
        eitherUsername,
        eitherPassword,
        eitherEmail
      ).mapN(DatabaseUser(UserId(0), _, _, _, user.role)).pure[F]
    } yield domainUser
  }

  private def validateUsername(username: String, userId: UserId): F[Either[UserValidationError, Username]] = {
    if (username.length < 8 || username.length > 32) {
      val error: UserValidationError = InvalidUsernameLength
      error.asLeft[Username].pure[F]
    } else if (!username.split("").forall(_.matches("[A-Za-z0-9]"))) {
      val error: UserValidationError = InvalidUsernameSymbols
      error.asLeft[Username].pure[F]
    } else {
      val validated = Username(username)
      for {
        userInBase <- userRepository.findByUsername(validated)
        result = userInBase match {
          case None                            => validated.asRight[UserValidationError]
          case Some(user) if user.id == userId => validated.asRight[UserValidationError]
          case Some(_)                         => UsernameInUse(validated).asLeft[Username]
        }
      } yield result
    }
  }

  private def validatePassword(password: String): F[Either[UserValidationError, EncryptedPassword]] = {
    if (password.length < 12 || password.length > 32) {
      val error: UserValidationError = InvalidPassword
      error.asLeft[EncryptedPassword].pure[F]
    } else {
      val rawPassword = Password(password)
      crypto.encrypt(rawPassword).asRight[UserValidationError].pure[F]
    }
  }

  private def validateEmail(email: String, userId: UserId): F[Either[UserValidationError, Email]] = {
    def validateEmailDuplicates(email: Email): F[Either[UserValidationError, Email]] = {
      for {
        option <- userRepository.findByEmail(email)
        either = option match {
          case None                            => email.asRight[UserValidationError]
          case Some(user) if user.id == userId => email.asRight[UserValidationError]
          case Some(_)                         => EmailInUse(email).asLeft[Email]
        }
      } yield either
    }

    val result = for {
      validatedEmail <- EitherT(
        validateParameter[UserValidationError, String, MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](
          email,
          InvalidEmail
        ).pure[F]
      )
      _ <- EitherT(validateEmailDuplicates(validatedEmail))
    } yield validatedEmail

    result.value
  }

  private def fulfillUser(user: DatabaseUser): F[FullUserDto] = {
    for {
      domainSuppliers  <- supplierRepository.findByUser(user)
      domainCategories <- categoryRepository.findByUser(user)

      dtoSuppliers  = domainSuppliers.map(supplierDomainToDto)
      dtoCategories = domainCategories.map(categoryDomainToDto)
    } yield userDomainToFullUserDto(user, dtoSuppliers, dtoCategories)
  }
}
