package com.itechart.project.configuration

import cats.effect.Sync
import com.itechart.project.configuration.ConfigurationTypes.MailConfiguration
import courier.Mailer

object MailSettings {

  def mailer[F[_]: Sync](configuration: MailConfiguration): F[Mailer] = {
    Sync[F].delay(
      Mailer(configuration.host, configuration.port)
        .auth(true)
        .as(configuration.sender, configuration.password)
        .startTls(true)()
    )
  }

}
