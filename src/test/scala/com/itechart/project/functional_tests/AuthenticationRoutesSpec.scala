package com.itechart.project.functional_tests

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.itechart.project.dto.auth.LoginUser
import com.itechart.project.functional_tests.Commons.{checkResponse, login, logout, postRequest}
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class AuthenticationRoutesSpec extends AnyFreeSpec with Matchers {

  "Authentication tests" - {

    implicit val userEntityEncoder: EntityEncoder[IO, LoginUser] = jsonEncoderOf[IO, LoginUser]

    "Correct login/logout test" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use(client =>
          for {
            token <- login(client, LoginUser("KolodkoNikita", "05082001Slonim"))
            _     <- logout(client, token)
          } yield ()
        )
        .unsafeRunSync()
    }

    "Incorrect username on login" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val user = LoginUser("Some wrong username", "123q456q")
          for {
            response <- postRequest(client, uri"http://localhost:8080/login", user)
            result    = checkResponse(response, Forbidden, Some(s"The user with username `${user.username}` is not found"))
          } yield result
        }
        .unsafeRunSync()
    }

    "Incorrect user password on login" in {
      BlazeClientBuilder[IO](ExecutionContext.global).resource
        .use { client =>
          val user = LoginUser("KolodkoNikita", "Wrong password :)")
          for {
            response <- postRequest(client, uri"http://localhost:8080/login", user)
            result    = checkResponse(response, Forbidden, Some(s"Invalid password for username `${user.username}`"))
          } yield result
        }
        .unsafeRunSync()
    }
  }

}
