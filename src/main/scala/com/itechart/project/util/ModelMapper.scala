package com.itechart.project.util

import com.itechart.project.domain.category.{CategoryId, DatabaseCategory}
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId}
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.supplier.SupplierDto
import eu.timepit.refined.predicates.all.NonEmpty
import io.scalaland.chimney.dsl.TransformerOps
import eu.timepit.refined.auto._

object ModelMapper {

  def categoryDomainToDto(category: DatabaseCategory): CategoryDto = {
    category
      .into[CategoryDto]
      .withFieldComputed(_.id, _.id.id)
      .withFieldComputed(_.name, _.name.value)
      .transform
  }

  def categoryDtoToDomain(categoryDto: CategoryDto): DatabaseCategory = {
    categoryDto
      .into[DatabaseCategory]
      .withFieldConst(_.id, CategoryId(categoryDto.id))
      .withFieldConst(_.name, RefinedConversion.convertParameter[String, NonEmpty](categoryDto.name, "Category"))
      .transform
  }

  def supplierDomainToDto(supplier: DatabaseSupplier): SupplierDto = {
    supplier
      .into[SupplierDto]
      .withFieldComputed(_.id, _.id.id)
      .withFieldComputed(_.name, _.name.value)
      .transform
  }

  def supplierDtoToDomain(supplierDto: SupplierDto): DatabaseSupplier = {
    supplierDto
      .into[DatabaseSupplier]
      .withFieldConst(_.id, SupplierId(supplierDto.id))
      .withFieldConst(_.name, RefinedConversion.convertParameter[String, NonEmpty](supplierDto.name, "Supplier"))
      .transform
  }

}
