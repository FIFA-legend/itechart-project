package com.itechart.project.domain

import eu.timepit.refined.types.string.NonEmptyString

object party {

  final case class PartyId(id: Long)

  type PartyName = NonEmptyString

  final case class DatabaseParty(id: PartyId, name: PartyName)

}
