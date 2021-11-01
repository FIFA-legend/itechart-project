package com.itechart.project.mailer

import cats.effect.Sync
import com.itechart.project.configuration.ConfigurationTypes.MailConfiguration
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.mailer.impl.CourierMailService
import courier.Mailer

import java.io.File
import scala.concurrent.Future

trait MailService[F[_]] {
  def sendMessageToUsers(
    users:      List[DatabaseUser],
    item:       DatabaseItem,
    attachment: Option[File] = None
  ): F[List[Future[Unit]]]
}

object MailService {
  def of[F[_]: Sync](mailer: Mailer, mailConfiguration: MailConfiguration): CourierMailService[F] =
    new CourierMailService[F](mailer, mailConfiguration)
}
