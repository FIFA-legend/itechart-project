package com.itechart.project.authentication

import cats.syntax.all._
import cats.effect.Sync
import com.itechart.project.configuration.ConfigurationTypes.TokenExpiration
import com.itechart.project.effects.JwtClock
import pdi.jwt.JwtClaim

trait JwtExpire[F[_]] {
  def expiresIn(claim: JwtClaim, token: TokenExpiration): F[JwtClaim]
}

object JwtExpire {
  def of[F[_]: Sync]: F[JwtExpire[F]] = JwtClock[F].utc.map { implicit clock =>
    new JwtExpire[F] {
      override def expiresIn(claim: JwtClaim, token: TokenExpiration): F[JwtClaim] = {
        Sync[F].delay(claim.issuedNow.expiresIn(token.value.toMillis))
      }
    }
  }
}
