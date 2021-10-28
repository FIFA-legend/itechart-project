package com.itechart.project.services.impl

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.cart.{CartId, DatabaseCart}
import com.itechart.project.domain.order.{Address, DatabaseOrder, DeliveryStatus, OrderId}
import com.itechart.project.domain.user.UserId
import com.itechart.project.dto.cart.CartDto
import com.itechart.project.dto.order.OrderDto
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.repository.{CartRepository, ItemRepository, OrderRepository, UserRepository}
import com.itechart.project.services.OrderService
import com.itechart.project.services.error.OrderErrors.OrderValidationError
import com.itechart.project.services.error.OrderErrors.OrderValidationError.{
  InvalidOrderAddress,
  InvalidOrderCart,
  InvalidOrderStatus,
  InvalidOrderUser,
  OrderCartIsPartOfAnotherOrder,
  OrderNotFound
}
import com.itechart.project.util.ModelMapper.{
  cartsDomainToCartDto,
  fullUserDtoToDomain,
  itemDomainToCartItemDto,
  orderDomainToDto
}
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.collection.NonEmpty
import io.chrisdavenport.log4cats.Logger
import squants.market.{Money, USD}

class OrderServiceImpl[F[_]: Sync: Logger](
  orderRepository: OrderRepository[F],
  cartRepository:  CartRepository[F],
  itemRepository:  ItemRepository[F],
  userRepository:  UserRepository[F]
) extends OrderService[F] {
  override def findAllOrders: F[List[OrderDto]] = {
    for {
      domainOrders <- orderRepository.all
      dtoOrders    <- domainOrders.map(fulfillOrder).sequence
    } yield dtoOrders
  }

  override def findAllByUser(userId: Long): F[Either[OrderValidationError, List[OrderDto]]] = {
    val res: EitherT[F, OrderValidationError, List[OrderDto]] = for {
      user             <- EitherT.fromOptionF(userRepository.findById(UserId(userId)), InvalidOrderUser(userId))
      userDomainOrders <- EitherT.liftF(orderRepository.findByUser(user))
      userDtoOrders    <- EitherT.liftF(userDomainOrders.map(fulfillOrder).sequence)
    } yield userDtoOrders

    res.value
  }

  override def findById(id: Long): F[Either[OrderValidationError, OrderDto]] = {
    val res: EitherT[F, OrderValidationError, OrderDto] = for {
      domainOrder <- EitherT.fromOptionF(orderRepository.findById(OrderId(id)), OrderNotFound(id))
      dtoOrder    <- EitherT.liftF(fulfillOrder(domainOrder))
    } yield dtoOrder

    res.value
  }

  override def createOrder(order: OrderDto, user: FullUserDto): F[Either[OrderValidationError, OrderDto]] = {
    val res: EitherT[F, OrderValidationError, OrderDto] = for {
      domainUser  <- EitherT.fromOptionF(userRepository.findById(UserId(user.id)), InvalidOrderUser(user.id))
      domainCarts <- EitherT(validateCart(order.cart))
      address     <- EitherT(validateAddress(order.address))
      total       <- EitherT.liftF(countOrderSum(domainCarts))

      domainOrder = DatabaseOrder(OrderId(0), Money(total, USD), address, DeliveryStatus.Ordered, domainUser.id)

      id       <- EitherT.liftF(orderRepository.create(domainOrder))
      _        <- EitherT.liftF(domainCarts.map(cart => cartRepository.update(cart.copy(orderId = Some(id)))).sequence)
      dtoOrder <- EitherT.liftF(fulfillOrder(domainOrder.copy(id = id)))
    } yield dtoOrder

    res.value
  }

  override def updateOrderToAssigned(id: Long): F[Either[OrderValidationError, Boolean]] = {
    updateStatus(id, DeliveryStatus.Assigned)
  }

  override def updateOrderToDelivered(id: Long): F[Either[OrderValidationError, Boolean]] = {
    updateStatus(id, DeliveryStatus.Delivered)
  }

  private def countOrderSum(carts: List[DatabaseCart]): F[Double] = {
    val fOptionItems = carts.map(itemRepository.findByCart).sequence
    for {
      optionList <- fOptionItems
      sum = optionList.sequence match {
        case None        => 0
        case Some(items) => carts.zip(items).map { case (cart, item) => cart.quantity.value * item.price.value }.sum
      }
    } yield sum
  }

  private def validateAddress(address: String): F[Either[OrderValidationError, Address]] = {
    validateParameter[OrderValidationError, String, NonEmpty](address, InvalidOrderAddress).pure[F]
  }

  private def validateCart(dtoCart: CartDto): F[Either[OrderValidationError, List[DatabaseCart]]] = {
    def isPartOfAnotherOrder(carts: List[DatabaseCart]): Boolean = {
      carts.find(_.orderId.isDefined) match {
        case Some(_) => true
        case None    => false
      }
    }

    val fList = dtoCart.items.map(cart => cartRepository.findById(CartId(cart.id))).sequence
    for {
      listOfOption <- fList
      either <- listOfOption.sequence match {
        case None => InvalidOrderCart.asLeft[List[DatabaseCart]].pure[F]
        case Some(value) if isPartOfAnotherOrder(value) =>
          OrderCartIsPartOfAnotherOrder.asLeft[List[DatabaseCart]].pure[F]
        case Some(value) => value.asRight[OrderValidationError].pure[F]
      }
    } yield either
  }

  private def updateStatus(id: Long, status: DeliveryStatus): F[Either[OrderValidationError, Boolean]] = {
    val res: EitherT[F, OrderValidationError, Boolean] = for {
      domainOrder <- EitherT.fromOptionF(orderRepository.findById(OrderId(id)), OrderNotFound(id))

      statusCheck = domainOrder.status match {
        case DeliveryStatus.Delivered => InvalidOrderStatus.asLeft[DeliveryStatus]
        case status                   => status.asRight[OrderValidationError]
      }
      _ <- EitherT.liftF(statusCheck.pure[F])

      updated <- EitherT.liftF(orderRepository.update(domainOrder.copy(status = status)))
    } yield updated != 0

    res.value
  }

  private def fulfillOrder(order: DatabaseOrder): F[OrderDto] = {
    for {
      domainCarts           <- cartRepository.findByOrder(order)
      listOptionDomainItems <- domainCarts.map(cart => itemRepository.findByCart(cart)).sequence

      domainItems = listOptionDomainItems.sequence match {
        case None        => List()
        case Some(value) => value
      }

      dtoItems = domainItems.map(itemDomainToCartItemDto)
      dtoCart  = cartsDomainToCartDto(domainCarts.zip(dtoItems))
    } yield orderDomainToDto(order, dtoCart)
  }

}
