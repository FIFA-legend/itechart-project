package com.itechart.project.repository.impl.meta

import com.itechart.project.domain.auth.Email
import doobie.Meta
import eu.timepit.refined.{refineV, W}
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.auto._

object MetaImplicits {
  private def convertParameter[T, P](
    parameter: T,
    default:   Refined[T, P]
  )(
    implicit v: Validate[T, P]
  ): Refined[T, P] = {
    refineV(parameter).getOrElse(default)
  }

  implicit val emailMeta: Meta[Email] =
    Meta[String].timap(
      convertParameter[String, MatchesRegex[W.`"^[A-Za-z0-9]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](_, "email@gmail.com")
    )(_.toString())
}
