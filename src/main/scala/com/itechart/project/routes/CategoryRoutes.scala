package com.itechart.project.routes

import cats.effect.Sync
import cats.implicits._
import com.itechart.project.dto.category.CategoryDto
import com.itechart.project.services.CategoryService
import com.itechart.project.services.error.CategoryErrors.CategoryValidationError
import com.itechart.project.services.error.CategoryErrors.CategoryValidationError.{
  CategoryAlreadyExists,
  CategoryNotFound,
  InvalidCategoryName
}
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}

import scala.util.Try

object CategoryRoutes {

  def routes[F[_]: Sync](categoryService: CategoryService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    def allCategories: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "categories" =>
      for {
        categories <- categoryService.findAllCategories
        response   <- Ok(categories)
      } yield response
    }

    def getCategory: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "categories" / LongVar(id) =>
      val res = for {
        found <- categoryService.findById(id)
      } yield found

      marshalResponse(res)
    }

    def createCategory: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "categories" =>
      val res = for {
        category <- req.as[CategoryDto]
        created  <- categoryService.createCategory(category)
      } yield created

      marshalResponse(res)
    }

    def updateCategory(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "categories" =>
      val res = for {
        category <- req.as[CategoryDto]
        updated  <- categoryService.updateCategory(category)
      } yield updated

      marshalResponse(res)
    }

    def deleteCategory(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "categories" / LongVar(id) =>
      val res = for {
        deleted <- categoryService.deleteCategory(id)
      } yield deleted

      marshalResponse(res)
    }

    object LongVar {
      def unapply(value: String): Option[Long] = Try(value.toLong).toOption
    }

    def categoryErrorToHttpResponse(error: CategoryValidationError): F[Response[F]] = {
      error match {
        case e: CategoryNotFound      => NotFound(e.message)
        case e: CategoryAlreadyExists => Conflict(e.message)
        case e @ InvalidCategoryName => BadRequest(e.message)

        case e => BadRequest(e.message)
      }
    }

    def marshalResponse[T](
      result: F[Either[CategoryValidationError, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] =
      result
        .flatMap {
          case Left(error) => categoryErrorToHttpResponse(error)
          case Right(dto)  => Ok(dto)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage)
        }

    allCategories <+> getCategory <+> updateCategory() <+> createCategory <+> deleteCategory()
  }

}