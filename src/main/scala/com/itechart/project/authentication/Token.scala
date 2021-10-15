package com.itechart.project.authentication

import cats.Monad
import cats.syntax.all._
import com.itechart.project.configuration.ConfigurationTypes.{JwtAccessTokenKeyConfiguration, TokenExpiration}
import com.itechart.project.effects.GeneratorUUID
import dev.profunktor.auth.jwt.{jwtEncode, JwtSecretKey, JwtToken}
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import io.circe.syntax._

trait Token[F[_]] {
  def create: F[JwtToken]
}

object Token {
  def of[F[_]: GeneratorUUID: Monad](
    jwtExpire:     JwtExpire[F],
    configuration: JwtAccessTokenKeyConfiguration,
    token:         TokenExpiration
  ): Token[F] = new Token[F] {
    override def create: F[JwtToken] = {
      for {
        uuid     <- GeneratorUUID[F].make
        claim    <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), token)
        secretKey = JwtSecretKey(configuration.secret.value)
        token    <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
      } yield token
    }
  }
}
