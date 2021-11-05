package com.itechart.project.services.error

object ItemErrors {

  sealed trait ItemValidationError extends ValidationError

  object ItemValidationError {
    // 404
    final case class ItemNotFound(itemId: Long) extends ItemValidationError {
      override def message: String = s"The item with id `$itemId` is not found"
    }

    // 400
    final case object InvalidItemName extends ItemValidationError {
      override def message: String = "Item name is empty"
    }

    // 400
    final case object InvalidItemDescription extends ItemValidationError {
      override def message: String = "Item description is empty"
    }

    // 400
    final case object InvalidItemAmount extends ItemValidationError {
      override def message: String = "Item amount is negative"
    }

    // 400
    final case object InvalidItemPrice extends ItemValidationError {
      override def message: String = "Item price is negative"
    }

    // 400
    final case class InvalidItemCategory(categoryId: Long) extends ItemValidationError {
      override def message: String = s"The category with id `$categoryId` doesn't exist"
    }

    // 400
    final case class InvalidItemSupplier(supplierId: Long) extends ItemValidationError {
      override def message: String = s"The supplier with id `$supplierId` doesn't exist"
    }

  }

}
