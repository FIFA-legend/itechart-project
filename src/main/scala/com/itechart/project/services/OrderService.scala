package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.dto.order.OrderDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.repository.{CartRepository, ItemRepository, OrderRepository, UserRepository}
import com.itechart.project.services.error.OrderErrors.OrderValidationError
import com.itechart.project.services.impl.OrderServiceImpl
import org.typelevel.log4cats.Logger

trait OrderService[F[_]] {
  def findAllOrders: F[List[OrderDto]]
  def findAllByUser(userId:      Long): F[Either[OrderValidationError, List[OrderDto]]]
  def findById(id:               Long): F[Either[OrderValidationError, OrderDto]]
  def createOrder(order:         OrderDto, user: FullUserDto): F[Either[OrderValidationError, OrderDto]]
  def updateOrderToAssigned(id:  Long): F[Either[OrderValidationError, Boolean]]
  def updateOrderToDelivered(id: Long): F[Either[OrderValidationError, Boolean]]
}

object OrderService {
  def of[F[_]: Sync: Logger](
    orderRepository: OrderRepository[F],
    cartRepository:  CartRepository[F],
    itemRepository:  ItemRepository[F],
    userRepository:  UserRepository[F]
  ): OrderService[F] =
    new OrderServiceImpl[F](
      orderRepository,
      cartRepository,
      itemRepository,
      userRepository
    )
}
