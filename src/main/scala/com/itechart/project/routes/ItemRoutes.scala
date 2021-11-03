package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.dto.item.{FilterItemDto, ItemDto}
import com.itechart.project.dto.user.FullUserDto
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.services.ItemService
import com.itechart.project.services.error.ItemErrors.ItemValidationError
import com.itechart.project.services.error.ItemErrors.ItemValidationError._
import io.circe.generic.JsonCodec
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityEncoder, HttpRoutes, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object ItemRoutes {

  def routes[F[_]: Sync: Logger: JsonDecoder](itemService: ItemService[F]): HttpRoutes[F] = {
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
        filter   <- req.asJsonDecode[FilterItemDto]
        items    <- itemService.findAllByFilter(filter)
        response <- Ok(items)
      } yield response
    }

    def getItem: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "items" / LongVar(id) =>
      val res = for {
        found <- itemService.findById(id)
      } yield found

      marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    allItems <+> getItem <+> allItemsFilter
  }

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](
    itemService: ItemService[F]
  ): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allAvailableItemsForUser: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case request @ GET -> Root / "items" / "available" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          for {
            user     <- request.req.asJsonDecode[FullUserDto]
            items    <- itemService.findAllByUser(user)
            response <- Ok(items)
          } yield response
        }
    }

    def allAvailableItemsForUserFilter: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case request @ GET -> Root / "items" / "available" / "filter" as user =>
        @JsonCodec final case class UserAndFilter(user: FullUserDto, filter: FilterItemDto)

        if (!isResourceAvailable(user.value.role, List(Role.Client))) Forbidden()
        else {
          for {
            userAndFilter <- request.req.asJsonDecode[UserAndFilter]
            items         <- itemService.findAllByUserAndFilter(userAndFilter.user, userAndFilter.filter)
            response      <- Ok(items)
          } yield response
        }
    }

    def createItem: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ POST -> Root / "items" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
      else {
        val res = for {
          item    <- request.req.asJsonDecode[ItemDto]
          created <- itemService.createItem(item)
        } yield created

        marshalResponse(res)
      }
    }

    def updateItem(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ PUT -> Root / "items" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
      else {
        val res = for {
          item    <- request.req.asJsonDecode[ItemDto]
          updated <- itemService.updateItem(item)
        } yield updated

        marshalResponse(res)
      }
    }

    def deleteItem(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case DELETE -> Root / "items" / LongVar(id) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            deleted <- itemService.deleteItem(id)
          } yield deleted

          marshalResponse(res)
        }
    }

    updateItem() <+> createItem <+> deleteItem() <+>
      allAvailableItemsForUser <+> allAvailableItemsForUserFilter
  }

  def itemErrorToHttpResponse[F[_]: Sync: Logger](error: ItemValidationError): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

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

  def marshalResponse[F[_]: Sync: Logger, T](
    result: F[Either[ItemValidationError, T]]
  )(
    implicit E: EntityEncoder[F, T]
  ): F[Response[F]] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    result
      .flatMap {
        case Left(error) => itemErrorToHttpResponse(error) <* Logger[F].warn(error.message)
        case Right(dto)  => Ok(dto)
      }
      .handleErrorWith { ex =>
        InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
      }
  }

}
