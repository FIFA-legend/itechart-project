package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.cart.{CartId, DatabaseCart}
import com.itechart.project.domain.item.{Amount, AvailabilityStatus, DatabaseItem, ItemId}
import com.itechart.project.domain.user.{DatabaseUser, UserId}
import com.itechart.project.dto.cart.{CartDto, SingleCartDto}
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.repository.{CartRepository, GroupRepository, ItemRepository, UserRepository}
import com.itechart.project.services.CartService
import com.itechart.project.services.error.CartErrors.CartValidationError
import com.itechart.project.services.error.CartErrors.CartValidationError.{
  CartIsPartOfOrder,
  CartItemIsNotAvailable,
  CartNotFound,
  CartQuantityIsOutOfBounds,
  InvalidCartItem,
  InvalidCartQuantity,
  InvalidCartUser,
  ItemIsAlreadyInCart
}
import com.itechart.project.util.ModelMapper.{
  cartDomainToDto,
  cartsDomainToCartDto,
  fullUserDtoToDomain,
  itemDomainToSimpleItemDto,
  singleCartDtoToDomain
}
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.numeric.GreaterEqual
import org.typelevel.log4cats.Logger

class CartServiceImpl[F[_]: Sync: Logger](
  cartRepository:  CartRepository[F],
  itemRepository:  ItemRepository[F],
  userRepository:  UserRepository[F],
  groupRepository: GroupRepository[F]
) extends CartService[F] {

  override def findByUser(user: FullUserDto): F[Either[CartValidationError, CartDto]] = {
    val result: EitherT[F, CartValidationError, CartDto] = for {
      _         <- EitherT.liftF(Logger[F].info(s"Selecting all carts from database for user with id = ${user.id}"))
      _         <- EitherT.fromOptionF(userRepository.findById(UserId(user.id)), InvalidCartUser(user.id))
      userDomain = fullUserDtoToDomain(user)

      cartsList <- EitherT.liftF(cartRepository.findCurrentCartsByUser(userDomain))
      itemsList <- EitherT(findItemToCart(cartsList))

      cartDto = cartsDomainToCartDto(cartsList.zip(itemsList.map(itemDomainToSimpleItemDto)))
      _ <- EitherT.liftF(
        Logger[F].info(s"Selected ${cartsList.size} carts from database for user with id = ${user.id}")
      )
    } yield cartDto

    result.value
  }

  override def createCart(cart: SingleCartDto, user: FullUserDto): F[Either[CartValidationError, SingleCartDto]] = {
    val result: EitherT[F, CartValidationError, SingleCartDto] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Creating new cart in database"))
      userDomain <- EitherT(validateUser(user.id))
      itemDomain <- EitherT(validateItem(cart.item.id, user))
      _          <- EitherT(validateItemDuplicates(userDomain, itemDomain))
      newAmount  <- EitherT(validateQuantityOnCreation(cart.quantity, itemDomain).pure[F])

      cartDomain    = singleCartDtoToDomain(cart, user)
      newItemDomain = itemDomain.copy(amount = newAmount)
      itemDto       = itemDomainToSimpleItemDto(newItemDomain)

      id          <- EitherT.liftF(cartRepository.create(cartDomain))
      _           <- EitherT.liftF(itemRepository.update(newItemDomain))
      returnValue <- EitherT.liftF(cartDomainToDto(cartDomain.copy(id = id), itemDto).pure[F])
      _           <- EitherT.liftF(Logger[F].info(s"New cart created successfully. It's id = $id"))
    } yield returnValue

    result.value
  }

  override def updateCart(cart: SingleCartDto, user: FullUserDto): F[Either[CartValidationError, SingleCartDto]] = {
    val result: EitherT[F, CartValidationError, SingleCartDto] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Updating cart with id = ${cart.id} in database"))
      _          <- EitherT(validateUser(user.id))
      cartDomain <- EitherT(validateCart(cart.id))
      itemDomain <- EitherT(validateItem(cart.item.id, user))

      newCartQuantity <- EitherT(
        validateParameter[CartValidationError, Int, GreaterEqual[1]](cart.quantity, InvalidCartQuantity).pure[F]
      )
      newItemAmount <- EitherT(validateQuantityOnUpdate(cart.quantity, cartDomain, itemDomain).pure[F])

      updated <- EitherT.liftF(cartRepository.update(cartDomain.copy(quantity = newCartQuantity)))
      _       <- EitherT.liftF(itemRepository.update(itemDomain.copy(amount = newItemAmount)))
      _       <- EitherT.liftF(Logger[F].info(s"Cart with id = ${cart.id} update status: ${updated != 0}"))
    } yield cart

    result.value
  }

  override def deleteCart(id: Long): F[Either[CartValidationError, Boolean]] = {
    val result: EitherT[F, CartValidationError, Boolean] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Deleting cart with id = $id from database"))
      cartDomain <- EitherT.fromOptionF(cartRepository.findById(CartId(id)), CartNotFound(id))
      itemDomain <- EitherT.fromOptionF(
        itemRepository.findById(cartDomain.itemId),
        InvalidCartItem(cartDomain.itemId.value)
      )
      _ <- EitherT.fromOption[F](
        if (cartDomain.orderId.isEmpty) Some(cartDomain) else None,
        CartIsPartOfOrder(id)
      )

      newItemAmount <- EitherT(
        validateParameter[CartValidationError, Int, GreaterEqual[0]](
          itemDomain.amount.value + cartDomain.quantity.value,
          CartQuantityIsOutOfBounds
        ).pure[F]
      )

      deleted <- EitherT.liftF(cartRepository.delete(CartId(id)))
      _       <- EitherT.liftF(itemRepository.update(itemDomain.copy(amount = newItemAmount)))
      _       <- EitherT.liftF(Logger[F].info(s"Cart with id = $id delete status: ${deleted != 0}"))
    } yield deleted != 0

    result.value
  }

  private def findItemToCart(carts: List[DatabaseCart]): F[Either[CartValidationError, List[DatabaseItem]]] = {
    for {
      listOption  <- carts.map(cart => itemRepository.findById(cart.itemId)).sequence
      itemsId      = carts.map(_.itemId.value)
      notFoundList = listOption.zip(itemsId)
      either <- notFoundList
        .map { case (option, id) =>
          EitherT.fromOption[F](option, InvalidCartItem(id))
        }
        .sequence
        .value
    } yield either
  }

  private def validateUser(userId: Long): F[Either[CartValidationError, DatabaseUser]] = {
    for {
      option <- userRepository.findById(UserId(userId))
      either = option match {
        case Some(value) => value.asRight[CartValidationError]
        case None        => InvalidCartUser(userId).asLeft[DatabaseUser]
      }
    } yield either
  }

  private def validateItemDuplicates(
    user: DatabaseUser,
    item: DatabaseItem
  ): F[Either[CartValidationError, DatabaseItem]] = {
    for {
      itemsInCart <- cartRepository.findCurrentCartsByUser(user)
      either =
        if (itemsInCart.contains(item)) ItemIsAlreadyInCart(item.id.value).asLeft[DatabaseItem]
        else item.asRight[CartValidationError]
    } yield either
  }

  private def validateItem(itemId: Long, user: FullUserDto): F[Either[CartValidationError, DatabaseItem]] = {
    for {
      option <- itemRepository.findById(ItemId(itemId))
      aItems <- itemRepository.findByStatus(AvailabilityStatus.Available)

      userDomain = fullUserDtoToDomain(user)
      uItems    <- itemRepository.findByUser(userDomain)

      userGroups <- groupRepository.findByUser(userDomain)
      gItems     <- userGroups.map(itemRepository.findByGroup).sequence

      either = option match {
        case None => InvalidCartItem(itemId).asLeft[DatabaseItem]
        case Some(value) if !aItems.contains(value) && !uItems.contains(value) && !gItems.contains(value) =>
          CartItemIsNotAvailable(itemId).asLeft[DatabaseItem]
        case Some(value) => value.asRight[CartValidationError]
      }
    } yield either
  }

  private def validateQuantityOnCreation(quantity: Int, item: DatabaseItem): Either[CartValidationError, Amount] = {
    for {
      _        <- validateParameter[CartValidationError, Int, GreaterEqual[1]](quantity, InvalidCartQuantity)
      newAmount = item.amount.value - quantity
      validatedNewAmount <- validateParameter[CartValidationError, Int, GreaterEqual[0]](
        newAmount,
        CartQuantityIsOutOfBounds
      )
    } yield validatedNewAmount
  }

  private def validateQuantityOnUpdate(
    quantity:     Int,
    previousCart: DatabaseCart,
    item:         DatabaseItem
  ): Either[CartValidationError, Amount] = {
    for {
      _ <- validateParameter[CartValidationError, Int, GreaterEqual[1]](quantity, InvalidCartQuantity)

      difference    = quantity - previousCart.quantity.value
      newItemAmount = item.amount.value - difference

      validatedNewItemAmount <- validateParameter[CartValidationError, Int, GreaterEqual[0]](
        newItemAmount,
        CartQuantityIsOutOfBounds
      )
    } yield validatedNewItemAmount
  }

  private def validateCart(cartId: Long): F[Either[CartValidationError, DatabaseCart]] = {
    for {
      option <- cartRepository.findById(CartId(cartId))
      either = option match {
        case Some(value) => value.asRight[CartValidationError]
        case None        => CartNotFound(cartId).asLeft[DatabaseCart]
      }
    } yield either
  }
}
