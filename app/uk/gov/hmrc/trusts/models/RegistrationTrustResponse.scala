/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.http.Status.{BAD_REQUEST, CONFLICT, FORBIDDEN, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}



case class RegistrationTrustResponse(trn : String)

object RegistrationTrustResponse {

  implicit val formats = Json.format[RegistrationTrustResponse]
  implicit lazy val httpReads: HttpReads[RegistrationTrustResponse] =
    new HttpReads[RegistrationTrustResponse] {
      override def read(method: String, url: String, response: HttpResponse): RegistrationTrustResponse = {
        Logger.info(s"[RegistrationTrustResponse]  response status received from des: ${response.status}")
        response.status match {
          case OK =>response.json.as[RegistrationTrustResponse]
          case FORBIDDEN =>{
            response.json.asOpt[DesErrorResponse] match {
              case Some(desReponse) if desReponse.code == "ALREADY_REGISTERED"=>
                throw new AlreadyRegisteredException
              case _ =>
                throw new InternalServerErrorException("Forbidden response from des.")
            }
          }
          case BAD_REQUEST => throw new  BadRequestException
          case SERVICE_UNAVAILABLE => throw new ServiceNotAvailableException("Des depdedent service is down.")
          case status =>  throw new InternalServerErrorException(s"Not handled status response from des $status")
        }
      }
    }
  implicit val registrationDesResponseReads = RegistrationDesResponse.formats
  implicit val registrationDesErrorResponseReads = DesErrorResponse.formats

}
case class RegistrationDesResponse(trn:String)
object RegistrationDesResponse {
  implicit val formats = Json.format[DesResponse]
}


