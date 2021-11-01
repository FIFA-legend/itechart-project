package com.itechart.project.mailer.impl

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.configuration.ConfigurationTypes.MailConfiguration
import com.itechart.project.domain.item.DatabaseItem
import com.itechart.project.domain.user.DatabaseUser
import com.itechart.project.mailer.MailService
import courier.Defaults._
import courier._

import java.io.File
import scala.concurrent.Future

class CourierMailService[F[_]: Sync](mailer: Mailer, mailConfiguration: MailConfiguration) extends MailService[F] {

  private val mailSubject: String = "New item is now available"

  override def sendMessageToUsers(
    users:      List[DatabaseUser],
    item:       DatabaseItem,
    attachment: Option[File] = None
  ): F[List[Future[Unit]]] = {
    attachment match {
      case Some(value) => users.map(sendMailWithAttachment(_, item, value)).sequence
      case None        => users.map(sendMailWithoutAttachment(_, item)).sequence
    }
  }

  private def sendMailWithAttachment(user: DatabaseUser, item: DatabaseItem, file: File): F[Future[Unit]] = {
    mailer(
      Envelope
        .from(mailConfiguration.sender.addr)
        .to(user.email.value.addr)
        .subject(mailSubject)
        .content(
          Multipart()
            .attach(file)
            .text(message(item))
        )
    ).pure[F]
  }

  private def sendMailWithoutAttachment(user: DatabaseUser, item: DatabaseItem): F[Future[Unit]] = {
    mailer(
      Envelope
        .from(mailConfiguration.sender.addr)
        .to(user.email.value.addr)
        .subject(mailSubject)
        .content(Text(message(item)))
    ).pure[F]
  }

  private def message(item: DatabaseItem): String =
    s"Item `${item.name.value}` is now available. Buy it for ${item.price.value}"

}
