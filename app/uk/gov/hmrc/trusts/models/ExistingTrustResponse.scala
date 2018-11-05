/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.trusts.models

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


sealed trait ExistingTrustResponse

object ExistingTrustResponse {

  final case object Matched extends ExistingTrustResponse
  final case object NotMatched extends ExistingTrustResponse
  final case object BadRequest extends ExistingTrustResponse
  final case object AlreadyRegistered extends ExistingTrustResponse
  final case object ServerError extends ExistingTrustResponse
  final case object ServiceUnavailable extends ExistingTrustResponse



  implicit lazy val httpReads: HttpReads[ExistingTrustResponse] =
    new HttpReads[ExistingTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): ExistingTrustResponse = {
        Logger.info(s"response status received: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[DesResponse].`match` match {
              case true => Matched
              case false =>  NotMatched
            }
          case CONFLICT => {
            response.json.asOpt[DesErrorResponse] match {
              case Some(desReponse) if desReponse.code == "ALREADY_REGISTERED"=> AlreadyRegistered
              case _ => ServerError
            }
          }

          case BAD_REQUEST => BadRequest
          case SERVICE_UNAVAILABLE => ServiceUnavailable
          case status =>  ServerError
        }
      }
    }
  implicit val desResponseReads = DesResponse.formats
  implicit val desErrorResponseReads = DesErrorResponse.formats

}


case class DesResponse(`match`:Boolean)
object DesResponse {
  implicit val formats = Json.format[DesResponse]
}


case class DesErrorResponse(code: String,reason: String )

object DesErrorResponse {
  implicit val formats = Json.format[DesErrorResponse]
}
