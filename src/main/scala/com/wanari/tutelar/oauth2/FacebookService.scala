package com.wanari.tutelar.oauth2

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import cats.MonadError
import com.wanari.tutelar.{AuthService, CsrfService}
import com.wanari.tutelar.util.HttpWrapper
import spray.json.RootJsonReader

class FacebookService[F[_]: MonadError[?[_], Throwable]](
    val config: OAuth2ConfigService[F]
)(
    implicit
    val authService: AuthService[F],
    val csrfService: CsrfService[F],
    val http: HttpWrapper[F]
) extends OAuth2Service[F] {
  import OAuth2Service._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  val TYPE            = "facebook"
  val redirectUriBase = Uri("https://www.facebook.com/v3.2/dialog/oauth")
  val tokenEndpoint   = Uri("https://graph.facebook.com/v3.2/oauth/access_token")
  val userEndpoint    = Uri("https://graph.facebook.com/me")

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest = {
    val endpoint = tokenEndpoint.withQuery(
      Uri.Query(entityHelper.getAsMap(selfRedirectUri.toString))
    )
    HttpRequest(
      HttpMethods.GET,
      endpoint,
      Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) :: Nil
    )
  }

  protected def getApi(token: TokenResponseHelper): F[IdAndRaw] = {
    implicit val idAndRawR: RootJsonReader[IdAndRaw] = idAndRawReader("id")

    val request =
      HttpRequest(
        HttpMethods.GET,
        userEndpoint,
        Authorization(OAuth2BearerToken(token.access_token)) ::
          Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) ::
          Nil
      )

    for {
      resp <- http.singleRequest(request)
      ret  <- http.unmarshalEntityTo[IdAndRaw](resp)
    } yield ret
  }

}