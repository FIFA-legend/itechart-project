package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.dto.item.{FilterItemDto, ItemDto}
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.mailer.MailService
import com.itechart.project.repository.{
  AttachmentRepository,
  CategoryRepository,
  GroupRepository,
  ItemRepository,
  SupplierRepository,
  UserRepository
}
import com.itechart.project.services.error.ItemErrors.ItemValidationError
import com.itechart.project.services.impl.ItemServiceImpl
import org.typelevel.log4cats.Logger

trait ItemService[F[_]] {
  def findAllItems: F[List[ItemDto]]
  def findAllByUser(user:          FullUserDto): F[List[ItemDto]]
  def findAllByFilter(filter:      FilterItemDto): F[List[ItemDto]]
  def findAllByUserAndFilter(user: FullUserDto, filter: FilterItemDto): F[List[ItemDto]]
  def findById(id:                 Long):        F[Either[ItemValidationError, ItemDto]]
  def createItem(item:             ItemDto):     F[Either[ItemValidationError, ItemDto]]
  def updateItem(item:             ItemDto):     F[Either[ItemValidationError, ItemDto]]
  def deleteItem(id:               Long):        F[Either[ItemValidationError, Boolean]]
}

object ItemService {
  def of[F[_]: Sync: Logger](
    itemRepository:       ItemRepository[F],
    categoryRepository:   CategoryRepository[F],
    supplierRepository:   SupplierRepository[F],
    attachmentRepository: AttachmentRepository[F],
    groupRepository:      GroupRepository[F],
    userRepository:       UserRepository[F],
    mailService:          MailService[F]
  ): ItemService[F] =
    new ItemServiceImpl[F](
      itemRepository,
      categoryRepository,
      supplierRepository,
      attachmentRepository,
      groupRepository,
      userRepository,
      mailService
    )
}
