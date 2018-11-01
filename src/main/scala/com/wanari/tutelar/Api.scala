package com.wanari.tutelar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.Future

trait Api {
  def route(): Route
}

object Api {
  val emptyRoute = Route(_.reject())

  def createRoute(api: Seq[Api]): Route = {
    api
      .map(_.route())
      .fold(Api.emptyRoute)(_ ~ _)
  }

  def createApi(services: Services[Future]): Route = {
    import services._
    import com.wanari.tutelar.oauth2.OAuth2Api._

    val api = Seq(
      new HealthCheckApi(),
      new GithubApi(),
      new FacebookApi(),
      new GoogleApi()
    )

    createRoute(api)
  }
}
