package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.dto.cart.{CartDto, SingleCartDto}
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.repository.{CartRepository, GroupRepository, ItemRepository, UserRepository}
import com.itechart.project.services.error.CartErrors.CartValidationError
import com.itechart.project.services.impl.CartServiceImpl
import io.chrisdavenport.log4cats.Logger

trait CartService[F[_]] {
  def findByUser(user: FullUserDto): F[Either[CartValidationError, CartDto]]
  def createCart(cart: SingleCartDto, user: FullUserDto): F[Either[CartValidationError, SingleCartDto]]
  def updateCart(cart: SingleCartDto, user: FullUserDto): F[Either[CartValidationError, SingleCartDto]]
  def deleteCart(id:   Long): F[Either[CartValidationError, Boolean]]
}

object CartService {
  def of[F[_]: Sync: Logger](
    cartRepository:  CartRepository[F],
    itemRepository:  ItemRepository[F],
    userRepository:  UserRepository[F],
    groupRepository: GroupRepository[F]
  ): CartService[F] =
    new CartServiceImpl[F](cartRepository, itemRepository, userRepository, groupRepository)
}
