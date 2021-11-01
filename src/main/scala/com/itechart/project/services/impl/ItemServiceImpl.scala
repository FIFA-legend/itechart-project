package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.category.{CategoryId, DatabaseCategory}
import com.itechart.project.domain.item.{Amount, AvailabilityStatus, DatabaseItem, ItemDescription, ItemId, ItemName}
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId}
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.item.{AttachmentIdDto, FilterItemDto, ItemDto}
import com.itechart.project.dto.supplier.SupplierDto
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
import com.itechart.project.services.ItemService
import com.itechart.project.services.error.ItemErrors.ItemValidationError
import com.itechart.project.services.error.ItemErrors.ItemValidationError.{
  InvalidItemAmount,
  InvalidItemCategory,
  InvalidItemDescription,
  InvalidItemName,
  InvalidItemPrice,
  InvalidItemSupplier,
  ItemNotFound
}
import com.itechart.project.util.ModelMapper.{
  categoryDomainToDto,
  categoryDtoToDomain,
  filterItemDtoToDomain,
  fullUserDtoToDomain,
  itemDomainToDto,
  itemDtoToDomain,
  supplierDomainToDto
}
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.collection.NonEmpty
import org.typelevel.log4cats.Logger
import squants.market.{Money, USD}

import java.io.File

class ItemServiceImpl[F[_]: Sync: Logger](
  itemRepository:       ItemRepository[F],
  categoryRepository:   CategoryRepository[F],
  supplierRepository:   SupplierRepository[F],
  attachmentRepository: AttachmentRepository[F],
  groupRepository:      GroupRepository[F],
  userRepository:       UserRepository[F],
  mail:                 MailService[F]
) extends ItemService[F] {
  private val path = "src/main/resources/attachments"

  override def findAllItems: F[List[ItemDto]] = {
    for {
      _        <- Logger[F].info(s"Selecting all items from database")
      items    <- itemRepository.all
      itemsDto <- items.traverse(itemToDto)
      _        <- Logger[F].info(s"Selected ${items.size} items from database")
    } yield itemsDto
  }

  override def findAllByUser(user: FullUserDto): F[List[ItemDto]] = {
    for {
      _                 <- Logger[F].info(s"Selecting all items from database for user with id = ${user.id}")
      availableItems    <- itemRepository.findByStatus(AvailabilityStatus.Available)
      notAvailableItems <- itemRepository.findByStatus(AvailabilityStatus.NotAvailable)
      userDomain         = fullUserDtoToDomain(user)
      userSpecialItems  <- itemRepository.findByUser(userDomain)

      userGroups <- groupRepository.findByUser(userDomain)
      groupItems <- userGroups.map(itemRepository.findByGroup).sequence

      allItems = availableItems
        .appendedAll(notAvailableItems)
        .appendedAll(userSpecialItems)
        .appendedAll(groupItems.flatten)
        .distinct
      itemsDto <- allItems.traverse(itemToDto)
      _        <- Logger[F].info(s"Selected ${allItems.size} items from database for user with id = ${user.id}")
    } yield itemsDto
  }

  override def findAllByFilter(filter: FilterItemDto): F[List[ItemDto]] = {
    for {
      _                <- Logger[F].info(s"Selecting all items from database by filter: $filter")
      filteredItems    <- itemRepository.filter(filterItemDtoToDomain(filter))
      filteredItemsDto <- filteredItems.traverse(itemToDto)
      _                <- Logger[F].info(s"Selected ${filteredItems.size} items from database by filter: $filter")
    } yield filteredItemsDto
  }

  def findAllByUserAndFilter(user: FullUserDto, filter: FilterItemDto): F[List[ItemDto]] = {
    for {
      _             <- Logger[F].info(s"Selecting all items from database for user with id = ${user.id} by filter: $filter")
      filteredItems <- findAllByFilter(filter)
      userItems     <- findAllByUser(user)
      result         = filteredItems.toSet.intersect(userItems.toSet)
      _ <- Logger[F].info(
        s"Selected ${result.size} items from database for user with id = ${user.id} by filter: $filter"
      )
    } yield result.toList
  }

  override def findById(id: Long): F[Either[ItemValidationError, ItemDto]] = {
    val result: EitherT[F, ItemValidationError, ItemDto] = for {
      _ <- EitherT.liftF(Logger[F].info(s"Selecting item with id = $id from database"))
      item <- EitherT.fromOptionF[F, ItemValidationError, DatabaseItem](
        itemRepository.findById(ItemId(id)),
        ItemNotFound(id)
      )
      dto <- EitherT.liftF(itemToDto(item))
      _   <- EitherT.liftF(Logger[F].info(s"Item with id = $id selected successfully"))
    } yield dto

    result.value
  }

  override def createItem(item: ItemDto): F[Either[ItemValidationError, ItemDto]] = {
    val result: EitherT[F, ItemValidationError, ItemDto] = for {
      _               <- EitherT.liftF(Logger[F].info(s"Creating new item in database"))
      _               <- EitherT(validateItem(item).pure[F])
      domainSupplier  <- EitherT(validateSupplier(item.supplier))
      _               <- EitherT(validateCategories(item.categories))
      domainItem       = itemDtoToDomain(item)
      domainCategories = item.categories.map(categoryDtoToDomain)

      id           <- EitherT.liftF(itemRepository.create(domainItem))
      _            <- EitherT.liftF(categoryRepository.createLinksToItem(domainItem, domainCategories))
      newDomainItem = domainItem.copy(id = id)
      returnValue  <- EitherT.liftF(itemToDto(newDomainItem))
      _ <- EitherT.liftF(
        sendNotification(newDomainItem, AvailabilityStatus.NotAvailable, domainCategories, domainSupplier)
      )
      _ <- EitherT.liftF(Logger[F].info(s"New item created successfully. It's id = $id"))
    } yield returnValue

    result.value
  }

  override def updateItem(item: ItemDto): F[Either[ItemValidationError, ItemDto]] = {
    val result: EitherT[F, ItemValidationError, ItemDto] = for {
      _               <- EitherT.liftF(Logger[F].info(s"Updating item with id = ${item.id} in database"))
      prevItem        <- EitherT.fromOptionF(itemRepository.findById(ItemId(item.id)), ItemNotFound(item.id))
      _               <- EitherT(validateItem(item).pure[F])
      domainSupplier  <- EitherT(validateSupplier(item.supplier))
      _               <- EitherT(validateCategories(item.categories))
      domainItem       = itemDtoToDomain(item)
      domainCategories = item.categories.map(categoryDtoToDomain)

      updatedItem  <- EitherT.liftF(itemRepository.update(domainItem))
      updatedLinks <- EitherT.liftF(categoryRepository.updateLinksToItem(domainItem, domainCategories))
      returnValue  <- EitherT.liftF(itemToDto(domainItem))
      _ <- EitherT.liftF(
        sendNotification(domainItem, prevItem.status, domainCategories, domainSupplier)
      )
      _ <- EitherT.liftF(Logger[F].info(s"Item with id = ${item.id} update status: ${updatedItem + updatedLinks != 0}"))
    } yield returnValue

    result.value
  }

  override def deleteItem(id: Long): F[Either[ItemValidationError, Boolean]] = {
    val result: EitherT[F, ItemValidationError, Boolean] = for {
      _    <- EitherT.liftF(Logger[F].info(s"Deleting item with id = $id from database"))
      item <- EitherT.fromOptionF(itemRepository.findById(ItemId(id)), ItemNotFound(id))

      _       <- EitherT.liftF(categoryRepository.deleteLinksToItem(item))
      deleted <- EitherT.liftF(itemRepository.delete(ItemId(id)))
      _       <- EitherT.liftF(Logger[F].info(s"Category with id = $id delete status: ${deleted != 0}"))
    } yield deleted != 0

    result.value
  }

  private def itemToDto(item: DatabaseItem): F[ItemDto] = {
    for {
      supplier    <- supplierRepository.findByItem(item)
      categories  <- categoryRepository.findByItem(item)
      attachments <- attachmentRepository.findByItem(item)

      supplierDto    = supplierDomainToDto(supplier)
      categoriesDto  = categories.map(c => categoryDomainToDto(c))
      attachmentsDto = attachments.map(attach => AttachmentIdDto(attach.id.value))

      itemDto = itemDomainToDto(item, supplierDto, categoriesDto, attachmentsDto)
    } yield itemDto
  }

  private def sendNotification(
    item:           DatabaseItem,
    previousStatus: AvailabilityStatus,
    categories:     List[DatabaseCategory],
    supplier:       DatabaseSupplier
  ): F[Unit] = {
    if (previousStatus != AvailabilityStatus.Available && item.status == AvailabilityStatus.Available) {
      for {
        attachments       <- attachmentRepository.findByItem(item)
        file               = attachments.headOption.map(attachment => new File(path + File.separator + attachment.link))
        usersByCategories <- categories.map(userRepository.findByCategory).sequence
        usersBySupplier   <- userRepository.findBySupplier(supplier)
        usersToSendMail    = (usersByCategories.flatten ++ usersBySupplier).distinct
        _                 <- mail.sendMessageToUsers(usersToSendMail, item, file)
      } yield ()
    } else {
      ().pure[F]
    }
  }

  private def validateItem(item: ItemDto): Either[ItemValidationError, ItemDto] = {
    for {
      _ <- validateName(item.name)
      _ <- validateDescription(item.description)
      _ <- validateAmount(item.amount)
      _ <- validatePrice(item.price)
    } yield item
  }

  private def validateName(name: String): Either[ItemValidationError, ItemName] = {
    validateParameter[ItemValidationError, String, NonEmpty](name, InvalidItemName)
  }

  private def validateDescription(description: String): Either[ItemValidationError, ItemDescription] = {
    validateParameter[ItemValidationError, String, NonEmpty](description, InvalidItemDescription)
  }

  private def validateAmount(amount: Int): Either[ItemValidationError, Amount] = {
    validateParameter(amount, InvalidItemAmount)
  }

  private def validatePrice(price: Double): Either[ItemValidationError, Money] = {
    if (price < 0) InvalidItemPrice.asLeft[Money]
    else Money(price, USD).asRight[ItemValidationError]
  }

  private def validateSupplier(supplier: SupplierDto): F[Either[ItemValidationError, DatabaseSupplier]] = {
    for {
      option <- supplierRepository.findById(SupplierId(supplier.id))
      either = option match {
        case Some(value) => value.asRight[ItemValidationError]
        case None        => InvalidItemSupplier(supplier.id).asLeft[DatabaseSupplier]
      }
    } yield either
  }

  private def validateCategories(categories: List[CategoryDto]): F[Either[ItemValidationError, List[CategoryDto]]] = {
    for {
      listOption  <- categories.map(category => categoryRepository.findById(CategoryId(category.id))).sequence
      categoriesId = categories.map(_.id)
      notFoundList = listOption.zip(categoriesId)
      notFound     = notFoundList.find { case (option, _) => option.isEmpty }
      either = notFound match {
        case Some((_, id)) => InvalidItemCategory(id).asLeft[List[CategoryDto]]
        case None          => categories.asRight[ItemValidationError]
      }
    } yield either
  }
}
