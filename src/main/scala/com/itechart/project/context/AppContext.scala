package com.itechart.project.context

import cats.effect.{Async, Blocker, ContextShift, Resource}
import cats.implicits._
import com.itechart.project.authentication.Crypto
import com.itechart.project.configuration.AuthenticationSettings
import com.itechart.project.configuration.ConfigurationTypes.{AppConfiguration, PasswordSalt, RedisConfiguration}
import com.itechart.project.configuration.DatabaseSettings.{migrator, transactor}
import com.itechart.project.modules.Security
import com.itechart.project.repository.{
  AttachmentRepository,
  CartRepository,
  CategoryRepository,
  GroupRepository,
  ItemRepository,
  OrderRepository,
  SupplierRepository,
  UserRepository
}
import com.itechart.project.routes.{
  AttachmentRoutes,
  CartRoutes,
  CategoryRoutes,
  ItemRoutes,
  OrderRoutes,
  SupplierRoutes,
  UserRoutes
}
import com.itechart.project.services.{
  AttachmentService,
  CartService,
  CategoryService,
  ItemService,
  OrderService,
  SupplierService,
  UserService
}
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import io.chrisdavenport.log4cats.Logger
import org.http4s.HttpApp
import org.http4s.implicits._
import eu.timepit.refined.auto._

object AppContext {

  def setUp[F[_]: ContextShift: Async: Logger](configuration: AppConfiguration): Resource[F, HttpApp[F]] = {
    for {
      tx <- transactor[F](configuration.db)

      migrator <- Resource.eval(migrator[F](configuration.db))
      _        <- Resource.eval(migrator.migrate())

      blocker <- Blocker[F]
      crypto  <- Resource.eval(Crypto.of[F](PasswordSalt("Nikita")))

      categoryRepository   = CategoryRepository.of[F](tx)
      supplierRepository   = SupplierRepository.of[F](tx)
      itemRepository       = ItemRepository.of[F](tx)
      groupRepository      = GroupRepository.of[F](tx)
      attachmentRepository = AttachmentRepository.of[F](tx)
      cartRepository       = CartRepository.of[F](tx)
      userRepository       = UserRepository.of[F](tx)
      orderRepository      = OrderRepository.of[F](tx)

      categoryService = CategoryService.of[F](categoryRepository)
      supplierService = SupplierService.of[F](supplierRepository)
      itemService = ItemService
        .of[F](itemRepository, categoryRepository, supplierRepository, attachmentRepository, groupRepository)
      attachmentService = AttachmentService.of(attachmentRepository)
      cartService       = CartService.of[F](cartRepository, itemRepository, userRepository, groupRepository)
      orderService      = OrderService.of[F](orderRepository, cartRepository, itemRepository, userRepository)
      userService       = UserService.of[F](userRepository, supplierRepository, categoryRepository, crypto)

      categoryRoutes   = CategoryRoutes.routes[F](categoryService)
      supplierRoutes   = SupplierRoutes.routes[F](supplierService)
      itemRoutes       = ItemRoutes.routes[F](itemService)
      attachmentRoutes = AttachmentRoutes.routes[F](attachmentService, blocker)
      cartRoutes       = CartRoutes.routes[F](cartService)
      orderRoutes      = OrderRoutes.routes[F](orderService)
      userRoutes       = UserRoutes.routes[F](userService)
    } yield {
      (categoryRoutes <+> supplierRoutes <+> itemRoutes
        <+> attachmentRoutes <+> cartRoutes <+> orderRoutes <+> userRoutes).orNotFound
    }

  }

}
