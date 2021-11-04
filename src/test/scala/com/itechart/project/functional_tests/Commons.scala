package com.itechart.project.functional_tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.itechart.project.dto.auth.LoginUser
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.Client
import org.http4s.client.dsl.io.http4sClientSyntaxMethod
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIString

object Commons extends Matchers {

  private val uri = uri"http://localhost:8080"

  implicit val userEntityEncoder: EntityEncoder[IO, LoginUser] = jsonEncoderOf[IO, LoginUser]
  implicit val jwtEntityDecoder:  EntityDecoder[IO, JwtToken]  = jsonOf[IO, JwtToken]

  def login(client: Client[IO], user: LoginUser): IO[JwtToken] = {
    for {
      token <- client.expect[JwtToken](Method.POST(user, uri / "login"))
    } yield token
  }

  def logout(client: Client[IO], token: JwtToken): IO[String] = {
    for {
      result <- client.expect[String](
        Method.POST(uri / "logout", Header.Raw(CIString("Authorization"), s"Bearer ${token.value}"))
      )
    } yield result
  }

  def getRequest[A](client: Client[IO], path: Uri)(implicit i: EntityDecoder[IO, A]): IO[A] = {
    for {
      response <- client.expect[A](Method.GET(path))
    } yield response
  }

  def getRequestWithBody[A, B](
    client: Client[IO],
    path:   Uri,
    body:   B
  )(
    implicit a: EntityDecoder[IO, A],
    b:          EntityEncoder[IO, B]
  ): IO[A] = {
    for {
      response <- client.expect[A](Method.GET(body, path))
    } yield response
  }

  def getRequestWithAuthAndBody[A, B](
    client: Client[IO],
    path:   Uri,
    body:   B,
    token:  JwtToken
  )(
    implicit a: EntityDecoder[IO, A],
    b:          EntityEncoder[IO, B]
  ): IO[A] = {
    for {
      response <- client.expect[A](
        Method.GET(body, path, Header.Raw(CIString("Authorization"), s"Bearer ${token.value}"))
      )
    } yield response
  }

  def simplePostRequestWithAuth[A, B](
    client: Client[IO],
    body:   B,
    path:   Uri,
    token:  JwtToken
  )(
    implicit a: EntityDecoder[IO, A],
    b:          EntityEncoder[IO, B]
  ): IO[A] = {
    for {
      result <- client.expect[A](
        Method.POST(body, path, Header.Raw(CIString("Authorization"), s"Bearer ${token.value}"))
      )
    } yield result
  }

  def postRequest[A](client: Client[IO], path: Uri, body: A)(implicit i: EntityEncoder[IO, A]): IO[Response[IO]] = {
    for {
      response <- client.toHttpApp.run(
        Request(method = Method.POST, uri = path).withEntity(body)
      )
    } yield response
  }

  def simplePutRequestWithAuth[A, B](
    client: Client[IO],
    body:   B,
    path:   Uri,
    token:  JwtToken
  )(
    implicit a: EntityDecoder[IO, A],
    b:          EntityEncoder[IO, B]
  ): IO[A] = {
    for {
      result <- client.expect[A](
        Method.PUT(body, path, Header.Raw(CIString("Authorization"), s"Bearer ${token.value}"))
      )
    } yield result
  }

  def simpleDeleteRequestWithAuth[A](
    client: Client[IO],
    path:   Uri,
    token:  JwtToken
  )(
    implicit a: EntityDecoder[IO, A]
  ): IO[A] = {
    for {
      result <- client.expect[A](
        Method.DELETE(path, Header.Raw(CIString("Authorization"), s"Bearer ${token.value}"))
      )
    } yield result
  }

  def checkResponse[A](
    actualResponse:         Response[IO],
    expectedStatus:         Status,
    expectedBody:           Option[A],
    expectedResponseCookie: Option[ResponseCookie] = None,
  )(
    implicit decoder: EntityDecoder[IO, A],
  ): Unit = (for {
    _ <- IO(actualResponse.status shouldBe expectedStatus)
    _ <- expectedBody match {
      case None       => actualResponse.body.compile.toVector.map(_ shouldBe empty)
      case Some(body) => actualResponse.as[A].map(_ shouldBe body)
    }
    _ <- expectedResponseCookie match {
      case None                 => IO.unit
      case Some(responseCookie) => IO(actualResponse.cookies should contain(responseCookie))
    }
  } yield ()).unsafeRunSync()

}
