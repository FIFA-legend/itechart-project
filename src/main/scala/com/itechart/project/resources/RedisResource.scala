package com.itechart.project.resources

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import com.itechart.project.configuration.ConfigurationTypes.{AuthenticationConfiguration, RedisConfiguration}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.MkRedis
import eu.timepit.refined.auto._
import org.typelevel.log4cats.Logger

sealed abstract class RedisResource[F[_]](
  val redis: RedisCommands[F, String, String]
)

object RedisResource {

  def make[F[_]: Concurrent: Logger: MkRedis](
    cfg: AuthenticationConfiguration
  ): Resource[F, RedisResource[F]] = {

    def checkRedisConnection(
      redis: RedisCommands[F, String, String]
    ): F[Unit] =
      redis.info.flatMap {
        _.get("redis_version").traverse_ { v =>
          Logger[F].info(s"Connected to Redis $v")
        }
      }

    def mkRedisResource(c: RedisConfiguration): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value).evalTap(checkRedisConnection)

    mkRedisResource(cfg.redisConfiguration).map(res => new RedisResource[F](res) {})
  }

}
