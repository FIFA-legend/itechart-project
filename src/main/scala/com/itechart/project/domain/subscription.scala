package com.itechart.project.domain

import eu.timepit.refined.refineV
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec

object subscription {

  @JsonCodec final case class CategoryId(id: Long)

  @JsonCodec final case class SupplierId(id: Long)

  type CategoryName = NonEmptyString

  type SupplierName = NonEmptyString

  implicit val nonEmptyStringEncoder: Encoder[CategoryName] =
    Encoder.encodeString.contramap[CategoryName](_.toString)
  implicit val nonEmptyStringDecoder: Decoder[CategoryName] =
    Decoder.decodeString.emap[CategoryName](str => refineV(str))

  @JsonCodec case class Category(id: CategoryId, name: CategoryName)

  @JsonCodec final case class Supplier(id: SupplierId, name: SupplierName)

}
