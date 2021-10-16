package com.itechart.project.domain

import com.itechart.project.domain.subscription.{CategoryId, SupplierId}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import eu.timepit.refined.refineV
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec
import squants.market.{Money, USD}

import scala.util.Try

object item {

  type ItemName        = NonEmptyString
  type ItemDescription = NonEmptyString

  @JsonCodec final case class ItemId(value: Long)

  sealed trait AvailabilityStatus extends EnumEntry

  object AvailabilityStatus extends Enum[AvailabilityStatus] with CirceEnum[AvailabilityStatus] {
    val values: IndexedSeq[AvailabilityStatus] = findValues

    final case object InProcessing extends AvailabilityStatus
    final case object Available extends AvailabilityStatus
    final case object NotAvailable extends AvailabilityStatus
  }

  implicit val moneyEncoder: Encoder[Money] =
    Encoder.encodeString.contramap[Money](_.amount.toString())
  implicit val moneyDecoder: Decoder[Money] =
    Decoder.decodeString.emap[Money](str => Try(Money.apply(BigDecimal(str), USD)).toEither.left.map(_.toString))

  implicit val nonEmptyStringEncoder: Encoder[ItemName] =
    Encoder.encodeString.contramap[ItemName](_.toString)
  implicit val nonEmptyStringDecoder: Decoder[ItemName] =
    Decoder.decodeString.emap[ItemName](str => refineV(str))

  @JsonCodec case class Item(
    id:          ItemId,
    name:        ItemName,
    description: ItemDescription,
    amount:      Int,
    price:       Money,
    status:      AvailabilityStatus,
    supplier:    SupplierId,
    category:    CategoryId
  )

}
