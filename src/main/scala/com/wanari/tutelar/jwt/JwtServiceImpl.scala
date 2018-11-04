package com.wanari.tutelar.jwt

import cats.MonadError
import pdi.jwt.algorithms.{JwtAsymmetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}

object JwtServiceImpl {
  def create[F[_]: MonadError[?[_], Throwable]](implicit configService: JwtConfigService[F]): F[JwtService[F]] = {
    import cats.syntax.applicative._
    import cats.syntax.flatMap._
    import com.wanari.tutelar.util.ApplicativeErrorSyntax._
    import spray.json._

    configService.getConfig.flatMap { config =>
      JwtAlgorithm
        .optionFromString(config.algorithm)
        .collect {
          case algo: JwtHmacAlgorithm       => (algo, config.secret, config.secret)
          case algo: JwtAsymmetricAlgorithm => (algo, config.privateKey, config.publicKey)
        }
        .map {
          case (algo, encodeKey, decodeKey) =>
            new JwtService[F] {
              override def encode(data: JsObject): F[String] = {
                val claim = JwtClaim(data.compactPrint).expiresIn(config.expirationTime.toSeconds)
                JwtSprayJson.encode(claim, encodeKey, algo).pure
              }

              override def decode(token: String): F[JsObject] = {
                algo match {
                  case a: JwtHmacAlgorithm => JwtSprayJson.decodeJson(token, decodeKey, Seq(a)).fold(_.raise, _.pure)
                  case a: JwtAsymmetricAlgorithm =>
                    JwtSprayJson.decodeJson(token, decodeKey, Seq(a)).fold(_.raise, _.pure)
                }
              }

              override def validate(token: String): F[Boolean] = {
                algo match {
                  case a: JwtHmacAlgorithm       => JwtSprayJson.isValid(token, decodeKey, Seq(a)).pure
                  case a: JwtAsymmetricAlgorithm => JwtSprayJson.isValid(token, decodeKey, Seq(a)).pure
                }
              }
            }
        }
        .pureOrRaise(new Exception()) //TODO: Error handling

    }
  }
}