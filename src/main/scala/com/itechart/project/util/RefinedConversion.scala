package com.itechart.project.util

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV

object RefinedConversion {

  def convertParameter[T, P](
    parameter: T,
    default:   Refined[T, P]
  )(
    implicit v: Validate[T, P]
  ): Refined[T, P] = {
    refineV(parameter).getOrElse(default)
  }

  def validateParameter[E, T, P](
    parameter: T,
    error:     E
  )(
    implicit v: Validate[T, P]
  ): Either[E, Refined[T, P]] = {
    refineV(parameter).left
      .map(_ => error)
  }

}
