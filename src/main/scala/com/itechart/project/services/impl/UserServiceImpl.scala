package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.authentication.Crypto
import com.itechart.project.domain.user.{DatabaseUser, Email, EncryptedPassword, Password, UserId, Username}
import com.itechart.project.dto.user.{FullUserDto, UserDto}
import com.itechart.project.repository.{CategoryRepository, SupplierRepository, UserRepository}
import com.itechart.project.services.UserService
import com.itechart.project.services.error.UserErrors.UserValidationError
import com.itechart.project.services.error.UserErrors.UserValidationError.{
  EmailInUse,
  InvalidEmail,
  InvalidPassword,
  InvalidUsernameLength,
  InvalidUsernameSymbols,
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
      domainUsers <- userRepository.all
      dtoUsers    <- domainUsers.map(fulfillUser).sequence
    } yield dtoUsers
  }

  override def findById(id: Long): F[Either[UserValidationError, FullUserDto]] = {
    val res: EitherT[F, UserValidationError, FullUserDto] = for {
      userDomain <- EitherT.fromOptionF(userRepository.findById(UserId(id)), UserNotFound(id))
      dtoUser    <- EitherT.liftF(fulfillUser(userDomain))
    } yield dtoUser

    res.value
  }

  override def createUser(user: UserDto): F[Either[UserValidationError, FullUserDto]] = {
    val result: EitherT[F, UserValidationError, FullUserDto] = for {
      domainUser <- EitherT(validateUser(user, UserId(0)))
      id         <- EitherT.liftF(userRepository.create(domainUser))
      dtoUser    <- EitherT.liftF(fulfillUser(domainUser.copy(id = id)))
    } yield dtoUser

    result.value
  }

  override def updateUser(id: Long, user: UserDto): F[Either[UserValidationError, Boolean]] = {
    val result: EitherT[F, UserValidationError, Boolean] = for {
      foundUser     <- EitherT.fromOptionF(userRepository.findById(UserId(id)), UserNotFound(id))
      newDomainUser <- EitherT(validateUser(user, foundUser.id))

      updated <- EitherT.liftF(userRepository.update(newDomainUser.copy(id = foundUser.id)))
    } yield updated != 0

    result.value
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
