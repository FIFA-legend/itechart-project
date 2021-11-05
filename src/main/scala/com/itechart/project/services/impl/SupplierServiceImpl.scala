package com.itechart.project.services.impl

import cats.data.EitherT
import cats.implicits._
import cats.effect.Sync
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId, SupplierName}
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.repository.SupplierRepository
import com.itechart.project.services.SupplierService
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError._
import com.itechart.project.util.ModelMapper._
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.collection.NonEmpty
import org.typelevel.log4cats.Logger

import java.sql.SQLIntegrityConstraintViolationException

class SupplierServiceImpl[F[_]: Sync: Logger](supplierRepository: SupplierRepository[F]) extends SupplierService[F] {
  override def findAllSuppliers: F[List[SupplierDto]] = {
    for {
      _         <- Logger[F].info(s"Selecting all suppliers from database")
      suppliers <- supplierRepository.all
      supplier  <- suppliers.traverse(s => supplierDomainToDto(s).pure[F])
      _         <- Logger[F].info(s"Selected ${suppliers.size} suppliers from database")
    } yield supplier
  }

  override def findById(id: Long): F[Either[SupplierValidationError, SupplierDto]] = {
    val result: EitherT[F, SupplierValidationError, SupplierDto] = for {
      _          <- EitherT.liftF(Logger[F].info(s"Selecting supplier with id = $id from database"))
      supplier   <- EitherT.fromOptionF(supplierRepository.findById(SupplierId(id)), SupplierNotFound(id))
      dtoSupplier = supplierDomainToDto(supplier)
      _          <- EitherT.liftF(Logger[F].info(s"Supplier with id = $id selected successfully"))
    } yield dtoSupplier

    result.value
  }

  override def createSupplier(supplier: SupplierDto): F[Either[SupplierValidationError, SupplierDto]] = {
    val result: EitherT[F, SupplierValidationError, SupplierDto] = for {
      _             <- EitherT.liftF(Logger[F].info(s"Creating new supplier in database"))
      _             <- EitherT(validateSupplier(supplier).pure[F])
      domainSupplier = supplierDtoToDomain(supplier)
      _             <- EitherT(existsByName(domainSupplier))

      id <- EitherT.liftF(supplierRepository.create(domainSupplier))
      _  <- EitherT.liftF(Logger[F].info(s"New supplier created successfully. It's id = $id "))
    } yield supplierDomainToDto(domainSupplier.copy(id = id))

    result.value
  }

  override def updateSupplier(supplier: SupplierDto): F[Either[SupplierValidationError, SupplierDto]] = {
    val result: EitherT[F, SupplierValidationError, SupplierDto] = for {
      _ <- EitherT.liftF(Logger[F].info(s"Updating supplier with id = ${supplier.id} in database"))
      _ <- EitherT.fromOptionF(supplierRepository.findById(SupplierId(supplier.id)), SupplierNotFound(supplier.id))
      _ <- EitherT(validateSupplier(supplier).pure[F])

      domainSupplier = supplierDtoToDomain(supplier)
      _             <- EitherT(existsByName(domainSupplier))

      updated <- EitherT.liftF(supplierRepository.update(domainSupplier))
      _       <- EitherT.liftF(Logger[F].info(s"Supplier with id = ${supplier.id} update status: ${updated != 0}"))
    } yield supplierDomainToDto(domainSupplier)

    result.value
  }

  override def deleteSupplier(id: Long): F[Either[SupplierValidationError, Boolean]] = {
    val result: EitherT[F, SupplierValidationError, Boolean] = for {
      _ <- EitherT.liftF(Logger[F].info(s"Deleting supplier with id = $id from database"))
      _ <- EitherT.fromOptionF(supplierRepository.findById(SupplierId(id)), SupplierNotFound(id))
      deleted <- EitherT(supplierRepository.delete(SupplierId(id)).attempt).leftMap {
        case _: SQLIntegrityConstraintViolationException => SupplierIsConnected(id)
        case error => UnsupportedSupplierError(error.getMessage)
      }
      _ <- EitherT.liftF(Logger[F].info(s"Supplier with id = $id delete status: ${deleted != 0}"))
    } yield deleted != 0

    result.value
  }

  private def existsByName(supplier: DatabaseSupplier): F[Either[SupplierValidationError, DatabaseSupplier]] = {
    supplierRepository.findByName(supplier.name).map {
      case None    => supplier.asRight[SupplierValidationError]
      case Some(_) => SupplierInUse(supplier.name.value).asLeft[DatabaseSupplier]
    }
  }

  private def validateSupplier(supplier: SupplierDto): Either[SupplierValidationError, SupplierDto] = {
    for {
      _ <- validateName(supplier.name)
    } yield supplier
  }

  private def validateName(name: String): Either[SupplierValidationError, SupplierName] = {
    validateParameter[SupplierValidationError, String, NonEmpty](name, InvalidSupplierName)
  }

}
