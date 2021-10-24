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
import com.itechart.project.repository.{
  AttachmentRepository,
  CategoryRepository,
  GroupRepository,
  ItemRepository,
  SupplierRepository
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
import io.chrisdavenport.log4cats.Logger
import squants.market.{Money, USD}

class ItemServiceImpl[F[_]: Sync: Logger](
  itemRepository:       ItemRepository[F],
  categoryRepository:   CategoryRepository[F],
  supplierRepository:   SupplierRepository[F],
  attachmentRepository: AttachmentRepository[F],
  groupRepository:      GroupRepository[F],
) extends ItemService[F] {
  override def findAllItems: F[List[ItemDto]] = {
    for {
      items    <- itemRepository.all
      itemsDto <- items.traverse(itemToDto)
    } yield itemsDto
  }

  override def findAllByUser(user: FullUserDto): F[List[ItemDto]] = {
    for {
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
    } yield itemsDto
  }

  override def findAllByFilter(filter: FilterItemDto): F[List[ItemDto]] = {
    for {
      filteredItems    <- itemRepository.filter(filterItemDtoToDomain(filter))
      filteredItemsDto <- filteredItems.traverse(itemToDto)
    } yield filteredItemsDto
  }

  def findAllByUserAndFilter(user: FullUserDto, filter: FilterItemDto): F[List[ItemDto]] = {
    for {
      filteredItems <- findAllByFilter(filter)
      userItems     <- findAllByUser(user)
      result         = filteredItems.toSet ++ userItems.toSet
    } yield result.toList
  }

  override def findById(id: Long): F[Either[ItemValidationError, ItemDto]] = {
    val result: EitherT[F, ItemValidationError, ItemDto] = for {
      item <- EitherT.fromOptionF[F, ItemValidationError, DatabaseItem](
        itemRepository.findById(ItemId(id)),
        ItemNotFound(id)
      )
      dto <- EitherT.liftF(itemToDto(item))
    } yield dto

    result.value
  }

  override def createItem(item: ItemDto): F[Either[ItemValidationError, ItemDto]] = {
    val result: EitherT[F, ItemValidationError, ItemDto] = for {
      _               <- EitherT(validateItem(item).pure[F])
      _               <- EitherT(validateSupplier(item.supplier))
      _               <- EitherT(validateCategories(item.categories))
      domainItem       = itemDtoToDomain(item)
      domainCategories = item.categories.map(categoryDtoToDomain)

      id          <- EitherT.liftF(itemRepository.create(domainItem))
      _           <- EitherT.liftF(categoryRepository.createLinksToItem(domainItem, domainCategories))
      returnValue <- EitherT.liftF(itemToDto(domainItem.copy(id = id)))
    } yield returnValue

    result.value
  }

  override def updateItem(item: ItemDto): F[Either[ItemValidationError, ItemDto]] = {
    val result: EitherT[F, ItemValidationError, ItemDto] = for {
      _               <- EitherT.fromOptionF(itemRepository.findById(ItemId(item.id)), ItemNotFound(item.id))
      _               <- EitherT(validateItem(item).pure[F])
      _               <- EitherT(validateSupplier(item.supplier))
      _               <- EitherT(validateCategories(item.categories))
      domainItem       = itemDtoToDomain(item)
      domainCategories = item.categories.map(categoryDtoToDomain)

      _           <- EitherT.liftF(itemRepository.update(domainItem))
      _           <- EitherT.liftF(categoryRepository.updateLinksToItem(domainItem, domainCategories))
      returnValue <- EitherT.liftF(itemToDto(domainItem))
    } yield returnValue

    result.value
  }

  override def deleteItem(id: Long): F[Either[ItemValidationError, Boolean]] = {
    val result: EitherT[F, ItemValidationError, Boolean] = for {
      item <- EitherT.fromOptionF(itemRepository.findById(ItemId(id)), ItemNotFound(id))

      _       <- EitherT.liftF(categoryRepository.deleteLinksToItem(item))
      deleted <- EitherT.liftF(itemRepository.delete(ItemId(id)))
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
      attachmentsDto = attachments.map(attach => AttachmentIdDto(attach.id.id))

      itemDto = itemDomainToDto(item, supplierDto, categoriesDto, attachmentsDto)
    } yield itemDto
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
