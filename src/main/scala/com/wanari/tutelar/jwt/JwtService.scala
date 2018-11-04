package com.wanari.tutelar.jwt

import spray.json.JsObject

trait JwtService[F[_]] {
  def encode(data: JsObject): F[String]
  def decode(token: String): F[JsObject]
  def validate(token: String): F[Boolean]
}