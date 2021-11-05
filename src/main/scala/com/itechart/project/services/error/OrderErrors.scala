package com.itechart.project.services.error

object OrderErrors {

  sealed trait OrderValidationError extends ValidationError

  object OrderValidationError {
    // 404
    final case class OrderNotFound(orderId: Long) extends OrderValidationError {
      override def message: String = s"The order with id `$orderId` is not found"
    }

    // 400
    final case object InvalidOrderAddress extends OrderValidationError {
      override def message: String = "Order address is empty"
    }

    // 400
    final case object InvalidOrderStatus extends OrderValidationError {
      override def message: String = "Order status is delivered already"
    }

    // 400
    final case object InvalidOrderCart extends OrderValidationError {
      override def message: String = s"The order contains item not added to cart"
    }

    // 400
    final case object OrderCartIsPartOfAnotherOrder extends OrderValidationError {
      override def message: String = s"The order contains cart of another order"
    }

    // 400
    final case class InvalidOrderUser(userId: Long) extends OrderValidationError {
      override def message: String = s"The user with id `$userId` doesn't exist"
    }
  }

}
