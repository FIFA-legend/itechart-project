package com.itechart.project.services.error

import com.itechart.project.domain.user.Username

object AuthErrors {

  sealed trait UserAuthenticationError extends ValidationError

  object UserAuthenticationError {
    final case class InvalidPassword(username: Username) extends UserAuthenticationError {
      override def message: String = s"Invalid password for username `${username.value}`"
    }

    final case class UserNotFound(username: Username) extends UserAuthenticationError {
      override def message: String = s"The user with username `${username.value}` is not found"
    }
  }

}
