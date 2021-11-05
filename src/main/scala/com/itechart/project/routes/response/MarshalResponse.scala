package com.itechart.project.routes.response

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.services.error.ValidationError
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, Response}
import org.typelevel.log4cats.Logger

object MarshalResponse {

  def marshalResponse[F[_]: Sync: Logger, E <: ValidationError, T](
    result: F[Either[E, T]],
    func:   E => F[Response[F]]
  )(
    implicit E: EntityEncoder[F, T]
  ): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    result
      .flatMap {
        case Left(error) => func(error) <* Logger[F].warn(error.message)
        case Right(dto)  => Ok(dto)
      }
      .handleErrorWith { ex =>
        InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
      }
  }

}
