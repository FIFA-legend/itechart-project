package com.itechart.project.util

import com.itechart.project.domain.category.{CategoryId, DatabaseCategory}
import com.itechart.project.domain.item.{DatabaseItem, ItemId}
import com.itechart.project.domain.supplier.{DatabaseSupplier, SupplierId}
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.dto.item.ItemDto
import com.itechart.project.dto.supplier.SupplierDto
import eu.timepit.refined.predicates.all.NonEmpty
import io.scalaland.chimney.dsl.TransformerOps
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.GreaterEqual
import squants.market.{Money, USD}

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

  def itemDomainToDto(item: DatabaseItem, supplier: SupplierDto, categories: List[CategoryDto]): ItemDto = {
    item
      .into[ItemDto]
      .withFieldComputed(_.id, _.id.value)
      .withFieldComputed(_.name, _.name.value)
      .withFieldComputed(_.description, _.description.value)
      .withFieldComputed(_.description, _.description.value)
      .withFieldComputed(_.price, _.price.amount.doubleValue)
      .withFieldComputed(_.amount, _.amount.value)
      .withFieldConst(_.supplier, supplier)
      .withFieldConst(_.categories, categories)
      .transform
  }

  def itemDtoToDomain(itemDto: ItemDto): DatabaseItem = {
    itemDto
      .into[DatabaseItem]
      .withFieldConst(_.id, ItemId(itemDto.id))
      .withFieldConst(_.name, RefinedConversion.convertParameter[String, NonEmpty](itemDto.name, "Item name"))
      .withFieldConst(_.price, Money(itemDto.price, USD))
      .withFieldConst(_.amount, RefinedConversion.convertParameter[Int, GreaterEqual[0]](itemDto.amount, Int.MaxValue))
      .withFieldConst(
        _.description,
        RefinedConversion.convertParameter[String, NonEmpty](itemDto.description, "Item description")
      )
      .transform
  }

}
