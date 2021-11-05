package com.itechart.project.repository.impl.meta

import com.itechart.project.domain.cart.Quantity
import com.itechart.project.domain.item.{Amount, AvailabilityStatus}
import com.itechart.project.domain.order.DeliveryStatus
import com.itechart.project.domain.user.{Email, Role}
import com.itechart.project.util.RefinedConversion.convertParameter
import com.itechart.project.util.SnakeStyleConversion.{normalizedSnakeCase, snakeToCamel}
import doobie.Meta
import eu.timepit.refined.W
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import squants.market.{Money, USD}

object MetaImplicits {
  implicit val nonEmptyStringMeta: Meta[NonEmptyString] =
    Meta[String].timap(convertParameter[String, NonEmpty](_, "Non empty string"))(_.toString())

  implicit val emailMeta: Meta[Email] =
    Meta[String].timap(
      convertParameter[String, MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](_, "email@gmail.com")
    )(_.toString())

  implicit val amountMeta: Meta[Amount] =
    Meta[String].timap(str => convertParameter[Int, GreaterEqual[0]](str.toInt, Int.MaxValue))(_.toString())

  implicit val quantityMeta: Meta[Quantity] =
    Meta[String].timap(str => convertParameter[Int, GreaterEqual[1]](str.toInt, Int.MaxValue))(_.toString())

  implicit val priceMeta: Meta[Money] =
    Meta[String].timap(str => Money.apply(BigDecimal(str), USD))(_.amount.toString())

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
