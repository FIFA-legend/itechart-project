package com.itechart.project.services.error

import com.itechart.project.domain.user.{Email, Username}

import scala.util.control.NoStackTrace

object UserErrors {

  sealed trait UserValidationError extends RuntimeException with NoStackTrace {
    def message: String
  }

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
  }

}
