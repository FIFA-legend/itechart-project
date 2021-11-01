package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.item.{FilterItemDto, ItemDto}
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.services.ItemService
import com.itechart.project.services.error.ItemErrors.ItemValidationError
import com.itechart.project.services.error.ItemErrors.ItemValidationError.{
  InvalidItemAmount,
  InvalidItemCategory,
  InvalidItemDescription,
  InvalidItemName,
  InvalidItemPrice,
  InvalidItemSupplier,
  ItemNotFound
}
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.JsonCodec
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.util.Try

object ItemRoutes {

  def routes[F[_]: Sync: Logger](itemService: ItemService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allItems: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "items" =>
      for {
        items    <- itemService.findAllItems
        response <- Ok(items)
      } yield response
    }

    def allItemsFilter: HttpRoutes[F] = HttpRoutes.of[F] { case req @ GET -> Root / "items" / "filter" =>
      for {
        filter   <- req.as[FilterItemDto]
        items    <- itemService.findAllByFilter(filter)
        response <- Ok(items)
      } yield response
    }

    def allAvailableItemsForUser: HttpRoutes[F] = HttpRoutes.of[F] { case req @ GET -> Root / "items" / "available" =>
      for {
        user     <- req.as[FullUserDto]
        items    <- itemService.findAllByUser(user)
        response <- Ok(items)
      } yield response
    }

    def allAvailableItemsForUserFilter: HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ GET -> Root / "items" / "available" / "filter" =>
        @JsonCodec final case class UserAndFilter(user: FullUserDto, filter: FilterItemDto)

        for {
          userAndFilter <- req.as[UserAndFilter]
          items         <- itemService.findAllByUserAndFilter(userAndFilter.user, userAndFilter.filter)
          response      <- Ok(items)
        } yield response
    }

    def getItem: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "items" / LongVar(id) =>
      val res = for {
        found <- itemService.findById(id)
      } yield found

      marshalResponse(res)
    }

    def createItem: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "items" =>
      val res = for {
        item    <- req.as[ItemDto]
        created <- itemService.createItem(item)
      } yield created

      marshalResponse(res)
    }

    def updateCategory(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "items" =>
      val res = for {
        item    <- req.as[ItemDto]
        updated <- itemService.updateItem(item)
      } yield updated

      marshalResponse(res)
    }

    def deleteItem(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "items" / LongVar(id) =>
      val res = for {
        deleted <- itemService.deleteItem(id)
      } yield deleted

      marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def itemErrorToHttpResponse(error: ItemValidationError): F[Response[F]] = {
      error match {
        case e: ItemNotFound => NotFound(e.message)
        case e @ InvalidItemName        => BadRequest(e.message)
        case e @ InvalidItemDescription => BadRequest(e.message)
        case e @ InvalidItemAmount      => BadRequest(e.message)
        case e @ InvalidItemPrice       => BadRequest(e.message)
        case e: InvalidItemCategory => BadRequest(e.message)
        case e: InvalidItemSupplier => BadRequest(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[ItemValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => itemErrorToHttpResponse(error) <* Logger[F].info("ERROR: " + error.message)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
        }

    allItems <+> getItem <+> updateCategory() <+> createItem <+>
      deleteItem() <+> allAvailableItemsForUser <+>
      allItemsFilter <+> allAvailableItemsForUserFilter
  }

}
