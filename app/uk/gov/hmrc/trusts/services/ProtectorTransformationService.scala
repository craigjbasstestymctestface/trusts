/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.trusts.services

import javax.inject.Inject
import play.api.libs.json.{JsObject, JsValue, __}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.InternalServerErrorException
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.{RemoveProtector, Success}
import uk.gov.hmrc.trusts.transformers._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

class ProtectorTransformationService @Inject()(transformationService: TransformationService)
                                              (implicit ec:ExecutionContext) extends JsonOperations {

  def removeProtector(utr: String, internalId: String, removeProtector: RemoveProtector)
                     (implicit hc: HeaderCarrier) : Future[Success.type] = {

    getTransformedTrustJson(utr, internalId)
      .map(findProtectorJson(_, removeProtector.`type`, removeProtector.index))
      .flatMap(Future.fromTry)
      .flatMap { protectorJson =>
        transformationService.addNewTransform (utr, internalId,
          RemoveProtectorsTransform(
            removeProtector.index,
            protectorJson,
            removeProtector.endDate,
            removeProtector.`type`
          )
        ).map(_ => Success)
      }
  }

  private def getTransformedTrustJson(utr: String, internalId: String)
                                     (implicit hc:HeaderCarrier): Future[JsObject] = {

    transformationService.getTransformedData(utr, internalId).flatMap {
      case TrustProcessedResponse(json, _) => Future.successful(json.as[JsObject])
      case _ => Future.failed(InternalServerErrorException("Trust is not in processed state."))
    }
  }

  private def findProtectorJson(json: JsValue, protectorType: String, index: Int): Try[JsObject] = {
    val path = (__ \ 'details \ 'trust \ 'entities \ 'protectors \ protectorType \ index).json
    json.transform(path.pick).fold(
      _ => Failure(InternalServerErrorException("Could not locate protector at index")),
      value => scala.util.Success(value.as[JsObject])
    )
  }
}