package com.itechart.project.services.impl

import cats.data.EitherT
import cats.implicits._
import cats.effect.Sync
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId, SupplierName}
import com.itechart.project.dto.supplier.SupplierDto
import com.itechart.project.repository.SupplierRepository
import com.itechart.project.services.SupplierService
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError
import com.itechart.project.services.error.SupplierErrors.SupplierValidationError.{
  InvalidSupplierName,
  SupplierAlreadyExists,
  SupplierNotFound
}
import com.itechart.project.util.ModelMapper._
import com.itechart.project.util.RefinedConversion.validateParameter
import eu.timepit.refined.collection.NonEmpty
import io.chrisdavenport.log4cats.Logger

class SupplierServiceImpl[F[_]: Sync: Logger](supplierRepository: SupplierRepository[F]) extends SupplierService[F] {
  override def findAllSuppliers: F[List[SupplierDto]] = {
    for {
      suppliers <- supplierRepository.all <* Logger[F].info(s"Selecting all suppliers from database")
      supplier  <- suppliers.traverse(c => supplierDomainToDto(c).pure[F])
    } yield supplier
  }

  override def findById(id: Long): F[Either[SupplierValidationError, SupplierDto]] = {
    val result: EitherT[F, SupplierValidationError, SupplierDto] = for {
      supplier <- EitherT.fromOptionF[F, SupplierValidationError, DatabaseSupplier](
        supplierRepository.findById(SupplierId(id)),
        SupplierNotFound(id)
      ) <* EitherT.liftF(Logger[F].info(s"Selecting supplier with id = $id from database"))
      dto = supplierDomainToDto(supplier)
    } yield dto

    result.value
  }

  override def createSupplier(supplier: SupplierDto): F[Either[SupplierValidationError, SupplierDto]] = {
    val result: EitherT[F, SupplierValidationError, SupplierDto] = for {
      _ <- EitherT(validateSupplier(supplier).pure[F]) <* EitherT.liftF(
        Logger[F].info(s"Creating new supplier in database")
      )
      domain = supplierDtoToDomain(supplier)
      _     <- EitherT(existsByName(domain))

      id <- EitherT.liftF(supplierRepository.create(domain))
    } yield supplierDomainToDto(domain.copy(id = id))

    result.value
  }

  override def updateSupplier(supplier: SupplierDto): F[Either[SupplierValidationError, SupplierDto]] = {

    val result: EitherT[F, SupplierValidationError, SupplierDto] = for {
      _ <- EitherT.fromOptionF(
        supplierRepository.findById(SupplierId(supplier.id)),
        SupplierNotFound(supplier.id)
      ) <* EitherT.liftF(
        Logger[F].info(s"Updating supplier with id = ${supplier.id} in database")
      )
      _     <- EitherT(validateSupplier(supplier).pure[F])
      domain = supplierDtoToDomain(supplier)
      _     <- EitherT(existsByName(domain))

      _ <- EitherT.liftF(supplierRepository.update(domain))
    } yield supplierDomainToDto(domain)

    result.value
  }

  override def deleteSupplier(id: Long): F[Either[SupplierValidationError, Boolean]] = {

    val result: EitherT[F, SupplierValidationError, Boolean] = for {
      _ <- EitherT.fromOptionF(supplierRepository.findById(SupplierId(id)), SupplierNotFound(id)) <* EitherT.liftF(
        Logger[F].info(s"Deleting supplier with id = $id from database")
      )
      deleted <- EitherT.liftF(supplierRepository.delete(SupplierId(id)))
    } yield deleted != 0

    result.value
  }

  private def existsByName(supplier: DatabaseSupplier): F[Either[SupplierValidationError, DatabaseSupplier]] = {
    supplierRepository.findByName(supplier.name).map {
      case None    => supplier.asRight[SupplierValidationError]
      case Some(_) => SupplierAlreadyExists(supplier.name.value).asLeft[DatabaseSupplier]
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
