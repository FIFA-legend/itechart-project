package com.itechart.project.resources

import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.implicits._
import com.itechart.project.configuration.ConfigurationTypes.{AuthenticationConfiguration, RedisConfiguration}
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.MkRedis
import io.chrisdavenport.log4cats.Logger
import eu.timepit.refined.auto._

sealed abstract class RedisResource[F[_]](
  val redis: RedisCommands[F, String, String]
)

object RedisResource {

  def make[F[_]: Concurrent: Logger: MkRedis](
    cfg: AuthenticationConfiguration
  ): F[RedisResource[F]] = {

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

    mkRedisResource(cfg.redisConfiguration).use(res => new RedisResource[F](res) {}.pure[F])
  }

}
