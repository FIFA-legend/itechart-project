package com.itechart.project.services.error

object CartErrors {

  sealed trait CartValidationError extends ValidationError

  object CartValidationError {
    // 404
    final case class CartNotFound(cartId: Long) extends CartValidationError {
      override def message: String = s"The cart with id `$cartId` not found"
    }

    // 400
    final case object InvalidCartQuantity extends CartValidationError {
      override def message: String = "The quantity is negative"
    }

    // 400
    final case object CartQuantityIsOutOfBounds extends CartValidationError {
      override def message: String = "The quantity of item in stock is less"
    }

    // 400
    final case class CartItemIsNotAvailable(itemId: Long) extends CartValidationError {
      override def message: String = s"The item with id `$itemId` isn't available"
    }

    //400
    final case class InvalidCartUser(userId: Long) extends CartValidationError {
      override def message: String = s"The user with id `$userId` doesn't exist"
    }

    //400
    final case class InvalidCartItem(itemId: Long) extends CartValidationError {
      override def message: String = s"The item with id `$itemId` doesn't exist"
    }

    //400
    final case class ItemIsAlreadyInCart(itemId: Long) extends CartValidationError {
      override def message: String = s"The item with id `$itemId` is already in cart"
    }

    final case class CartIsPartOfOrder(cartId: Long) extends CartValidationError {
      override def message: String = s"The cart with id `$cartId` is part of order"
    }
  }

}
