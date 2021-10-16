package com.itechart.project.repository.impl.meta

import com.itechart.project.domain.item.{AvailabilityStatus, ItemDescription}
import com.itechart.project.domain.order.DeliveryStatus
import com.itechart.project.domain.subscription.CategoryName
import com.itechart.project.domain.user.{Email, Role}
import com.itechart.project.util.RefinedConversion.convertParameter
import com.itechart.project.util.SnakeStyleConversion.{normalizedSnakeCase, snakeToCamel}
import doobie.Meta
import eu.timepit.refined.W
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.auto._
import eu.timepit.refined.string.MatchesRegex
import squants.market.{Money, USD}

object MetaImplicits {
  implicit val categoryNameMeta: Meta[CategoryName] =
    Meta[String].timap(convertParameter[String, NonEmpty](_, "Non empty string"))(_.toString())

  implicit val emailMeta: Meta[Email] =
    Meta[String].timap(
      convertParameter[String, MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](_, "email@gmail.com")
    )(_.toString())

  implicit val priceMeta: Meta[Money] =
    Meta[String].timap(str => Money.apply(BigDecimal(str), USD))(_.toString())

  implicit val roleMeta: Meta[Role] =
    Meta[String].timap(str => Role.withNameInsensitive(snakeToCamel(str.toLowerCase)))(status =>
      normalizedSnakeCase(status.toString)
    )

  implicit val availabilityStatusMeta: Meta[AvailabilityStatus] =
    Meta[String].timap(str => AvailabilityStatus.withNameInsensitive(snakeToCamel(str.toLowerCase)))(status =>
      normalizedSnakeCase(status.toString)
    )

  implicit val deliveryStatusMeta: Meta[DeliveryStatus] =
    Meta[String].timap(str => DeliveryStatus.withNameInsensitive(snakeToCamel(str.toLowerCase)))(status =>
      normalizedSnakeCase(status.toString)
    )
}
