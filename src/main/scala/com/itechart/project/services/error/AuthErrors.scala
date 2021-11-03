package com.itechart.project.services.error

import com.itechart.project.domain.user.Username

import scala.util.control.NoStackTrace

object AuthErrors {

  sealed trait UserAuthenticationError extends RuntimeException with NoStackTrace {
    def message: String
  }

  object UserAuthenticationError {
    final case class InvalidPassword(username: Username) extends UserAuthenticationError {
      override def message: String = s"Invalid password for username `${username.value}`"
    }

    final case class UserNotFound(username: Username) extends UserAuthenticationError {
      override def message: String = s"The user with username `${username.value}` is not found"
    }

    final case object UnsupportedOperation extends UserAuthenticationError {
      override def message: String = s"Unsupported operation"
    }

    final case object TokenNotFound extends UserAuthenticationError {
      override def message: String = s"Jwt token is not found"
    }
  }

}
