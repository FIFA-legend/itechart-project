package com.itechart.project.context

import cats.effect.{Async, Blocker, ContextShift, Resource}
import cats.implicits.{toFlatMapOps, toFoldableOps, toSemigroupKOps}
import com.itechart.project.configuration.AuthenticationSettings
import com.itechart.project.configuration.ConfigurationTypes.{AppConfiguration, RedisConfiguration}
import com.itechart.project.configuration.DatabaseSettings.{migrator, transactor}
import com.itechart.project.modules.Security
import com.itechart.project.repository.{
  AttachmentRepository,
  CategoryRepository,
  GroupRepository,
  ItemRepository,
  SupplierRepository
}
import com.itechart.project.routes.{AttachmentRoutes, CategoryRoutes, ItemRoutes, SupplierRoutes}
import com.itechart.project.services.{AttachmentService, CategoryService, ItemService, SupplierService}
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import io.chrisdavenport.log4cats.Logger
import org.http4s.HttpApp
import org.http4s.implicits._

object AppContext {

  def setUp[F[_]: ContextShift: Async: Logger](configuration: AppConfiguration): Resource[F, HttpApp[F]] = {
    for {
      tx <- transactor[F](configuration.db)

      migrator <- Resource.eval(migrator[F](configuration.db))
      _        <- Resource.eval(migrator.migrate())

      blocker <- Blocker[F]

      categoryRepository   = CategoryRepository.of[F](tx)
      supplierRepository   = SupplierRepository.of[F](tx)
      itemRepository       = ItemRepository.of[F](tx)
      groupRepository      = GroupRepository.of[F](tx)
      attachmentRepository = AttachmentRepository.of[F](tx)

      categoryService = CategoryService.of[F](categoryRepository)
      supplierService = SupplierService.of[F](supplierRepository)
      itemService = ItemService
        .of[F](itemRepository, categoryRepository, supplierRepository, attachmentRepository, groupRepository)
      attachmentService = AttachmentService.of(attachmentRepository, itemRepository)

      categoryRoutes   = CategoryRoutes.routes[F](categoryService)
      supplierRoutes   = SupplierRoutes.routes[F](supplierService)
      itemRoutes       = ItemRoutes.routes[F](itemService)
      attachmentRoutes = AttachmentRoutes.routes[F](attachmentService, blocker)
    } yield (categoryRoutes <+> supplierRoutes <+> itemRoutes <+> attachmentRoutes).orNotFound
  }

}
