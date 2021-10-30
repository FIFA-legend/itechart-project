package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.group.GroupDto
import com.itechart.project.services.GroupService
import com.itechart.project.services.error.GroupValidationErrors.GroupValidationError
import com.itechart.project.services.error.GroupValidationErrors.GroupValidationError.{
  GroupNameInUse,
  GroupNotFound,
  InvalidGroupName,
  ItemIsInGroup,
  ItemNotFound,
  UserIsInGroup,
  UserNotFound
}
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.util.Try

object GroupRoutes {

  def routes[F[_]: Sync](groupService: GroupService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allGroups: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "groups" =>
      for {
        groups   <- groupService.findAllGroups
        response <- Ok(groups)
      } yield response
    }

    def allGroupsByUser: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "groups" / "user" / LongVar(userId) =>
      val res = for {
        foundGroups <- groupService.findAllByUser(userId)
      } yield foundGroups

      marshalResponse(res)
    }

    def allGroupsByItem: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "groups" / "item" / LongVar(itemId) =>
      val res = for {
        foundGroups <- groupService.findAllByItem(itemId)
      } yield foundGroups

      marshalResponse(res)
    }

    def getGroup: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "groups" / LongVar(id) =>
      val res = for {
        found <- groupService.findById(id)
      } yield found

      marshalResponse(res)
    }

    def createGroup: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "groups" =>
      val res = for {
        group   <- req.as[GroupDto]
        created <- groupService.createGroup(group)
      } yield created

      marshalResponse(res)
    }

    def updateGroup(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "groups" =>
      val res = for {
        group   <- req.as[GroupDto]
        updated <- groupService.updateGroup(group)
      } yield updated

      marshalResponse(res)
    }

    def deleteGroup(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "groups" / LongVar(id) =>
      val res = for {
        isDeleted <- groupService.deleteGroup(id)
      } yield isDeleted

      marshalResponse(res)
    }

    def addUserToGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "groups" / LongVar(groupId) / "user" / LongVar(userId) / "add" =>
        val res = for {
          isSuccessfullyAdded <- groupService.addUserToGroup(groupId, userId)
        } yield isSuccessfullyAdded

        marshalResponse(res)
    }

    def removeUserFromGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "groups" / LongVar(groupId) / "user" / LongVar(userId) / "remove" =>
        val res = for {
          isSuccessfullyRemoved <- groupService.removeUserFromGroup(groupId, userId)
        } yield isSuccessfullyRemoved

        marshalResponse(res)
    }

    def addItemToGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "groups" / LongVar(groupId) / "item" / LongVar(itemId) / "add" =>
        val res = for {
          isSuccessfullyAdded <- groupService.addItemToGroup(groupId, itemId)
        } yield isSuccessfullyAdded

        marshalResponse(res)
    }

    def removeItemFromGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "groups" / LongVar(groupId) / "item" / LongVar(itemId) / "remove" =>
        val res = for {
          isSuccessfullyRemoved <- groupService.removeItemFromGroup(groupId, itemId)
        } yield isSuccessfullyRemoved

        marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def groupErrorToHttpResponse(error: GroupValidationError): F[Response[F]] = {
      error match {
        case e: GroupNotFound  => NotFound(e.message)
        case e: GroupNameInUse => Conflict(e.message)
        case e: UserNotFound   => BadRequest(e.message)
        case e: ItemNotFound   => BadRequest(e.message)
        case e: UserIsInGroup  => Conflict(e.message)
        case e: ItemIsInGroup  => Conflict(e.message)
        case e @ InvalidGroupName => BadRequest(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[GroupValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => groupErrorToHttpResponse(error)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage)
        }

    allGroups <+> allGroupsByUser <+> allGroupsByItem <+> getGroup <+> createGroup <+> updateGroup() <+>
      deleteGroup() <+> addUserToGroup() <+> removeUserFromGroup() <+> addItemToGroup() <+> removeItemFromGroup()
  }

}
