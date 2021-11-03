package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.domain.user.Role
import com.itechart.project.dto.auth.LoggedInUser
import com.itechart.project.dto.group.GroupDto
import com.itechart.project.routes.access.AccessChecker.isResourceAvailable
import com.itechart.project.services.GroupService
import com.itechart.project.services.error.GroupErrors.GroupValidationError
import com.itechart.project.services.error.GroupErrors.GroupValidationError._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.{toMessageSyntax, JsonDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityEncoder, Response}
import org.typelevel.log4cats.Logger

import scala.util.Try

object GroupRoutes {

  def securedRoutes[F[_]: Sync: Logger: JsonDecoder](groupService: GroupService[F]): AuthedRoutes[LoggedInUser, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allGroups: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case GET -> Root / "groups" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
      else {
        for {
          groups   <- groupService.findAllGroups
          response <- Ok(groups)
        } yield response
      }
    }

    def allGroupsByUser: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case GET -> Root / "groups" / "user" / LongVar(userId) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager, Role.Client))) Forbidden()
        else {
          val res = for {
            foundGroups <- groupService.findAllByUser(userId)
          } yield foundGroups

          marshalResponse(res)
        }
    }

    def allGroupsByItem: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case GET -> Root / "groups" / "item" / LongVar(itemId) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            foundGroups <- groupService.findAllByItem(itemId)
          } yield foundGroups

          marshalResponse(res)
        }
    }

    def getGroup: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case GET -> Root / "groups" / LongVar(id) as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager, Role.Client))) Forbidden()
      else {
        val res = for {
          found <- groupService.findById(id)
        } yield found

        marshalResponse(res)
      }
    }

    def createGroup: AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of { case request @ POST -> Root / "groups" as user =>
      if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
      else {
        val res = for {
          group   <- request.req.asJsonDecode[GroupDto]
          created <- groupService.createGroup(group)
        } yield created

        marshalResponse(res)
      }
    }

    def updateGroup(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case request @ PUT -> Root / "groups" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            group   <- request.req.asJsonDecode[GroupDto]
            updated <- groupService.updateGroup(group)
          } yield updated

          marshalResponse(res)
        }
    }

    def deleteGroup(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case DELETE -> Root / "groups" / LongVar(id) as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            isDeleted <- groupService.deleteGroup(id)
          } yield isDeleted

          marshalResponse(res)
        }
    }

    def addUserToGroup(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case PUT -> Root / "groups" / LongVar(groupId) / "user" / LongVar(userId) / "add" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            isSuccessfullyAdded <- groupService.addUserToGroup(groupId, userId)
          } yield isSuccessfullyAdded

          marshalResponse(res)
        }
    }

    def removeUserFromGroup(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case PUT -> Root / "groups" / LongVar(groupId) / "user" / LongVar(userId) / "remove" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            isSuccessfullyRemoved <- groupService.removeUserFromGroup(groupId, userId)
          } yield isSuccessfullyRemoved

          marshalResponse(res)
        }
    }

    def addItemToGroup(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case PUT -> Root / "groups" / LongVar(groupId) / "item" / LongVar(itemId) / "add" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            isSuccessfullyAdded <- groupService.addItemToGroup(groupId, itemId)
          } yield isSuccessfullyAdded

          marshalResponse(res)
        }
    }

    def removeItemFromGroup(): AuthedRoutes[LoggedInUser, F] = AuthedRoutes.of {
      case PUT -> Root / "groups" / LongVar(groupId) / "item" / LongVar(itemId) / "remove" as user =>
        if (!isResourceAvailable(user.value.role, List(Role.Manager))) Forbidden()
        else {
          val res = for {
            isSuccessfullyRemoved <- groupService.removeItemFromGroup(groupId, itemId)
          } yield isSuccessfullyRemoved

          marshalResponse(res)
        }
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
          case Left(error) => groupErrorToHttpResponse(error) <* Logger[F].warn(error.message)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage) <* Logger[F].error(ex.getMessage)
        }

    allGroups <+> allGroupsByUser <+> allGroupsByItem <+> getGroup <+> createGroup <+> updateGroup() <+>
      deleteGroup() <+> addUserToGroup() <+> removeUserFromGroup() <+> addItemToGroup() <+> removeItemFromGroup()
  }

}
