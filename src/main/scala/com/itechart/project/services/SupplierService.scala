package com.itechart.project.services

import cats.effect.Sync
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.repository.SupplierRepository
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError
import com.itechart.project.services.impl.SupplierServiceImpl
import org.typelevel.log4cats.Logger

trait SupplierService[F[_]] {
  def findAllSuppliers: F[List[SupplierDto]]
  def findById(id:             Long):        F[Either[SupplierValidationError, SupplierDto]]
  def createSupplier(supplier: SupplierDto): F[Either[SupplierValidationError, SupplierDto]]
  def updateSupplier(supplier: SupplierDto): F[Either[SupplierValidationError, SupplierDto]]
  def deleteSupplier(id:       Long):        F[Either[SupplierValidationError, Boolean]]
}

object SupplierService {
  def of[F[_]: Sync: Logger](supplierRepository: SupplierRepository[F]): SupplierService[F] =
    new SupplierServiceImpl[F](supplierRepository)
}
