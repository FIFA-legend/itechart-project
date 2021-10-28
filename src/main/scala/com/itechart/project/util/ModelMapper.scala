package com.itechart.project.util

import com.itechart.project.domain.cart.{CartId, DatabaseCart}
import com.itechart.project.domain.category.{CategoryId, DatabaseCategory}
import com.itechart.project.domain.item.{DatabaseItem, DatabaseItemFilter, ItemId}
import com.itechart.project.domain.order.OrderId
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId}
import com.itechart.project.domain.user.{DatabaseUser, EncryptedPassword, Role, UserId, Username}
import com.itechart.project.dto.auth.{AuthUser, AuthUserWithPassword}
import com.itechart.project.dto.cart.{CartDto, CartItemDto, SingleCartDto}
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.item.{AttachmentIdDto, FilterItemDto, ItemDto}
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.dto.user.FullUserDto
import eu.timepit.refined.W
import eu.timepit.refined.predicates.all.NonEmpty
import io.scalaland.chimney.dsl.TransformerOps
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.string.MatchesRegex
import squants.market.{Money, USD}

object ModelMapper {

  def categoryDomainToDto(category: DatabaseCategory): CategoryDto = {
    category
      .into[CategoryDto]
      .withFieldComputed(_.id, _.id.id)
      .withFieldComputed(_.name, _.name.value)
      .transform
  }

  def categoryDtoToDomain(categoryDto: CategoryDto): DatabaseCategory = {
    categoryDto
      .into[DatabaseCategory]
      .withFieldConst(_.id, CategoryId(categoryDto.id))
      .withFieldConst(_.name, RefinedConversion.convertParameter[String, NonEmpty](categoryDto.name, "Category"))
      .transform
  }

  def supplierDomainToDto(supplier: DatabaseSupplier): SupplierDto = {
    supplier
      .into[SupplierDto]
      .withFieldComputed(_.id, _.id.id)
      .withFieldComputed(_.name, _.name.value)
      .transform
  }

  def supplierDtoToDomain(supplierDto: SupplierDto): DatabaseSupplier = {
    supplierDto
      .into[DatabaseSupplier]
      .withFieldConst(_.id, SupplierId(supplierDto.id))
      .withFieldConst(_.name, RefinedConversion.convertParameter[String, NonEmpty](supplierDto.name, "Supplier"))
      .transform
  }

  def itemDomainToDto(
    item:        DatabaseItem,
    supplier:    SupplierDto,
    categories:  List[CategoryDto],
    attachments: List[AttachmentIdDto]
  ): ItemDto = {
    item
      .into[ItemDto]
      .withFieldComputed(_.id, _.id.value)
      .withFieldComputed(_.name, _.name.value)
      .withFieldComputed(_.description, _.description.value)
      .withFieldComputed(_.description, _.description.value)
      .withFieldComputed(_.price, _.price.amount.doubleValue)
      .withFieldComputed(_.amount, _.amount.value)
      .withFieldConst(_.supplier, supplier)
      .withFieldConst(_.categories, categories)
      .withFieldConst(_.attachments, attachments)
      .transform
  }

  def itemDtoToDomain(itemDto: ItemDto): DatabaseItem = {
    itemDto
      .into[DatabaseItem]
      .withFieldConst(_.id, ItemId(itemDto.id))
      .withFieldConst(_.name, RefinedConversion.convertParameter[String, NonEmpty](itemDto.name, "Item name"))
      .withFieldConst(_.price, Money(itemDto.price, USD))
      .withFieldConst(_.amount, RefinedConversion.convertParameter[Int, GreaterEqual[0]](itemDto.amount, Int.MaxValue))
      .withFieldConst(
        _.description,
        RefinedConversion.convertParameter[String, NonEmpty](itemDto.description, "Item description")
      )
      .transform
  }

  def filterItemDtoToDomain(filterItemDto: FilterItemDto): DatabaseItemFilter = {
    filterItemDto
      .into[DatabaseItemFilter]
      .withFieldConst(_.categories, filterItemDto.categories.map(category => CategoryId(category.id)))
      .withFieldConst(_.suppliers, filterItemDto.suppliers.map(supplier => SupplierId(supplier.id)))
      .transform
  }

  def userDomainToDto(user: DatabaseUser): AuthUserWithPassword = {
    user
      .into[AuthUserWithPassword]
      .withFieldComputed(_.email, _.email.value)
      .transform
  }

  def authUserDtoToDomain(userDto: AuthUserWithPassword): DatabaseUser = {
    userDto
      .into[DatabaseUser]
      .withFieldConst(
        _.email,
        RefinedConversion.convertParameter[String, MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](
          userDto.email,
          "some@email.com"
        )
      )
      .withFieldConst(_.role, Role.Client)
      .transform
  }

  def fullUserDtoToDomain(fullUser: FullUserDto): DatabaseUser = {
    fullUser
      .into[DatabaseUser]
      .withFieldConst(_.id, UserId(fullUser.id))
      .withFieldConst(_.username, Username(fullUser.username))
      .withFieldConst(_.password, EncryptedPassword(""))
      .withFieldConst(
        _.email,
        RefinedConversion.convertParameter[String, MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](
          fullUser.email,
          "some@email.com"
        )
      )
      .transform
  }

  def singleCartDtoToDomain(cartItem: SingleCartDto, user: FullUserDto): DatabaseCart = {
    cartItem
      .into[DatabaseCart]
      .withFieldConst(_.id, CartId(cartItem.id))
      .withFieldConst(_.quantity, RefinedConversion.convertParameter[Int, GreaterEqual[1]](cartItem.quantity, 1))
      .withFieldConst(_.orderId, cartItem.orderId.map(OrderId))
      .withFieldConst(_.itemId, ItemId(cartItem.item.id))
      .withFieldConst(_.userId, UserId(user.id))
      .transform
  }

  def cartDomainToDto(cart: DatabaseCart, itemDto: CartItemDto): SingleCartDto = {
    cart
      .into[SingleCartDto]
      .withFieldComputed(_.id, _.id.id)
      .withFieldComputed(_.quantity, _.quantity.value)
      .withFieldComputed(_.orderId, _.orderId.map(_.value))
      .withFieldConst(_.item, itemDto)
      .transform
  }

  def cartsDomainToCartDto(list: List[(DatabaseCart, CartItemDto)]): CartDto = {
    val convertedList = list.map { case (cart, item) =>
      cartDomainToDto(cart, item)
    }
    CartDto(convertedList)
  }

  def itemDomainToCartItemDto(item: DatabaseItem): CartItemDto = {
    item
      .into[CartItemDto]
      .withFieldComputed(_.id, _.id.value)
      .withFieldComputed(_.price, _.price.amount.doubleValue)
      .withFieldComputed(_.name, _.name.value)
      .withFieldComputed(_.description, _.description.value)
      .transform
  }

}
