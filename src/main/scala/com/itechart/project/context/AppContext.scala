package com.itechart.project.context

import cats.effect.{Async, ContextShift, Resource}
import cats.implicits.toSemigroupKOps
import com.itechart.project.configuration.ConfigurationTypes.AppConfiguration
import com.itechart.project.configuration.DatabaseSettings.{migrator, transactor}
import com.itechart.project.repository.{CategoryRepository, SupplierRepository}
import com.itechart.project.routes.{CategoryRoutes, SupplierRoutes}
import com.itechart.project.services.{CategoryService, SupplierService}
import io.chrisdavenport.log4cats.Logger
import org.http4s.HttpApp
import org.http4s.implicits._

object AppContext {

  def setUp[F[_]: ContextShift: Async: Logger](configuration: AppConfiguration): Resource[F, HttpApp[F]] = {
    for {
      tx <- transactor[F](configuration.db)

      migrator <- Resource.eval(migrator[F](configuration.db))
      _        <- Resource.eval(migrator.migrate())

      categoryRepository = CategoryRepository.of[F](tx)
      supplierRepository = SupplierRepository.of[F](tx)

      categoryService = CategoryService.of[F](categoryRepository)
      supplierService = SupplierService.of[F](supplierRepository)

      categoryRoutes = CategoryRoutes.routes[F](categoryService)
      supplierRoutes = SupplierRoutes.routes[F](supplierService)
    } yield (categoryRoutes <+> supplierRoutes).orNotFound
  }

}
