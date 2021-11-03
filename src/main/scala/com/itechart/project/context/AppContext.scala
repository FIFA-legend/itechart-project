package com.itechart.project.context

import cats.effect.{Async, Resource}
import cats.implicits._
import com.itechart.project.authentication.{Crypto, JwtExpire, Token}
import com.itechart.project.configuration.AuthenticationSettings
import com.itechart.project.configuration.ConfigurationTypes.AppConfiguration
import com.itechart.project.configuration.DatabaseSettings.{migrator, transactor}
import com.itechart.project.configuration.MailSettings.mailer
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.mailer.MailService
import com.itechart.project.modules.Security
import com.itechart.project.repository._
import com.itechart.project.resources.RedisResource
import com.itechart.project.routes._
import com.itechart.project.services._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.redis4cats.effect.MkRedis
import org.http4s.HttpApp
import org.http4s.implicits._
import org.typelevel.log4cats.Logger

object AppContext {

  def setUp[F[_]: Async: Logger: MkRedis](configuration: AppConfiguration): Resource[F, HttpApp[F]] = {
    for {
      tx <- transactor[F](configuration.db)

      migrator <- Resource.eval(migrator[F](configuration.db))
      _        <- Resource.eval(migrator.migrate())

      mailer <- Resource.eval(mailer(configuration.mail))

      authentication <- Resource.eval(AuthenticationSettings.of[F])
      crypto         <- Resource.eval(Crypto.of[F](authentication.salt.value))
      redisResource  <- RedisResource.make[F](authentication)
      jwtExpire      <- Resource.eval(JwtExpire.of[F])
      token <- Resource.eval(
        Token.of[F](jwtExpire, authentication.tokenConfiguration.value, authentication.tokenExpiration).pure[F]
      )
      security <- Resource.eval(Security.of[F](authentication, tx, redisResource.redis))

      categoryRepository   = CategoryRepository.of[F](tx)
      supplierRepository   = SupplierRepository.of[F](tx)
      itemRepository       = ItemRepository.of[F](tx)
      groupRepository      = GroupRepository.of[F](tx)
      attachmentRepository = AttachmentRepository.of[F](tx)
      cartRepository       = CartRepository.of[F](tx)
      userRepository       = UserRepository.of[F](tx)
      orderRepository      = OrderRepository.of[F](tx)

      authService     = Auth.of(authentication.tokenExpiration, token, userRepository, redisResource.redis, crypto)
      mailService     = MailService.of(mailer, configuration.mail)
      categoryService = CategoryService.of[F](categoryRepository)
      supplierService = SupplierService.of[F](supplierRepository)
      itemService = ItemService
        .of[F](
          itemRepository,
          categoryRepository,
          supplierRepository,
          attachmentRepository,
          groupRepository,
          userRepository,
          mailService
        )
      attachmentService = AttachmentService.of(attachmentRepository)
      cartService       = CartService.of[F](cartRepository, itemRepository, userRepository, groupRepository)
      orderService      = OrderService.of[F](orderRepository, cartRepository, itemRepository, userRepository)
      userService       = UserService.of[F](userRepository, supplierRepository, categoryRepository, crypto)
      groupService      = GroupService.of[F](groupRepository, userRepository, itemRepository)

      userMiddleware = JwtAuthMiddleware[F, LoggedInUser](security.userJwtAuth.value, security.userAuth.findUser)

      categoryRoutes   = CategoryRoutes.routes[F](categoryService)
      supplierRoutes   = SupplierRoutes.routes[F](supplierService)
      itemRoutes       = ItemRoutes.routes[F](itemService)
      attachmentRoutes = AttachmentRoutes.routes[F](attachmentService)
      userRoutes       = UserRoutes.routes[F](userService)
      loginRoutes      = LoginRoutes.routes[F](authService)

      securedCategoryRoutes   = CategoryRoutes.securedRoutes[F](categoryService)
      securedSupplierRoutes   = SupplierRoutes.securedRoutes[F](supplierService)
      securedItemRoutes       = ItemRoutes.securedRoutes[F](itemService)
      securedAttachmentRoutes = AttachmentRoutes.securedRoutes[F](attachmentService)
      securedCartRoutes       = CartRoutes.securedRoutes[F](cartService)
      securedOrderRoutes      = OrderRoutes.securedRoutes[F](orderService)
      securedUserRoutes       = UserRoutes.securedRoutes[F](userService)
      securedGroupRoutes      = GroupRoutes.securedRoutes[F](groupService)
      securedLoginRoutes      = LoginRoutes.securedRoutes[F](authService)

      openRoutes = categoryRoutes <+> supplierRoutes <+> itemRoutes <+>
        attachmentRoutes <+> userRoutes <+> loginRoutes

      securedRoutes = userMiddleware(
        securedCategoryRoutes <+> securedSupplierRoutes <+> securedItemRoutes <+>
          securedAttachmentRoutes <+> securedCartRoutes <+> securedOrderRoutes <+>
          securedUserRoutes <+> securedGroupRoutes <+> securedLoginRoutes
      )
    } yield (openRoutes <+> securedRoutes).orNotFound
  }

}
