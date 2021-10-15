package com.itechart.project.effects

import cats.ApplicativeThrow
import cats.effect.Sync

import java.util.UUID

trait GeneratorUUID[F[_]] {
  def make: F[UUID]
  def read(string: String): F[UUID]
}

object GeneratorUUID {
  def apply[F[_]: GeneratorUUID]: GeneratorUUID[F] = implicitly

  implicit def generator[F[_]: Sync]: GeneratorUUID[F] = new GeneratorUUID[F] {
    override def make: F[UUID] = Sync[F].delay(UUID.randomUUID())

    override def read(string: String): F[UUID] = ApplicativeThrow[F].catchNonFatal(UUID.fromString(string))
  }
}
