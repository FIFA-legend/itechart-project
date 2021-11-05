package com.itechart.project.services.error

import com.itechart.project.domain.user.{Email, Username}

object UserErrors {

  sealed trait UserValidationError extends ValidationError

  object UserValidationError {
    // 404
    final case class UserNotFound(userId: Long) extends UserValidationError {
      override def message: String = s"The user with id `$userId` is not found"
    }

    // 400
    final case object InvalidUsernameSymbols extends UserValidationError {
      override def message: String = s"Username must contains only alphabetic symbols or digits"
    }

    // 400
    final case object InvalidUsernameLength extends UserValidationError {
      override def message: String = s"Username must be between 8 and 32 symbols"
    }

    // 409
    final case class UsernameInUse(username: Username) extends UserValidationError {
      override def message: String = s"Username `${username.value}` is taken already"
    }

    // 400
    final case object InvalidPassword extends UserValidationError {
      override def message: String = s"Password must be between 12 and 32 symbols"
    }

    // 400
    final case object InvalidEmail extends UserValidationError {
      override def message: String = s"Email is not valid"
    }

    // 409
    final case class EmailInUse(email: Email) extends UserValidationError {
      override def message: String = s"Email `${email.value}` is taken already"
    }

    // 404
    final case class SupplierNotFound(supplierId: Long) extends UserValidationError {
      override def message: String = s"The supplier with id `$supplierId` is not found"
    }

    // 404
    final case class CategoryNotFound(categoryId: Long) extends UserValidationError {
      override def message: String = s"The category with id `$categoryId` is not found"
    }

    // 409
    final case class SupplierIsSubscribed(userId: Long, supplierId: Long) extends UserValidationError {
      override def message: String = s"The user with id `$userId` subscribed supplier with id `$supplierId` already"
    }

    // 409
    final case class CategoryIsSubscribed(userId: Long, categoryId: Long) extends UserValidationError {
      override def message: String = s"The user with id `$userId` subscribed category with id `$categoryId` already"
    }
  }

}
