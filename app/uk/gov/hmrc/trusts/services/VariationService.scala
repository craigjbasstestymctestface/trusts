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
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.exceptions.{EtmpCacheDataStaleException, InternalServerErrorException}
import uk.gov.hmrc.trusts.models.DeclarationForApi
import uk.gov.hmrc.trusts.models.auditing.TrustAuditing
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.TrustProcessedResponse
import uk.gov.hmrc.trusts.models.variation.VariationResponse
import uk.gov.hmrc.trusts.transformers.DeclareNoChangeTransformer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationService @Inject()(desService: DesService, declareNoChangeTransformer: DeclareNoChangeTransformer, auditService: AuditService) {

  def submitDeclareNoChange(utr: String, internalId: String, declaration: DeclarationForApi)(implicit hc: HeaderCarrier): Future[VariationResponse] = {
    val checkedResponse = for {
      response <- desService.getTrustInfo(utr, internalId)
      fbn <- desService.getTrustInfoFormBundleNo(utr)
    } yield response match {
      case tpr:TrustProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
        Logger.info(s"[VariationService][submitDeclareNoChange] returning TrustProcessedResponse")
        response.asInstanceOf[TrustProcessedResponse]
      case _:TrustProcessedResponse =>
        Logger.info(s"[VariationService][submitDeclareNoChange] ETMP cached data in mongo has become stale, rejecting submission")
        throw EtmpCacheDataStaleException
      case _ =>
        Logger.warn(s"[VariationService][submitDeclareNoChange] Trust was not in a processed state")
        throw InternalServerErrorException("Submission could not proceed, Trust data was not in a processed state")
    }

    checkedResponse.flatMap { response =>
      declareNoChangeTransformer.transform(response, declaration) match {
        case JsSuccess(value, _) =>
          Logger.info(s"[VariationService][submitDeclareNoChange] Transformed json to declare no change, submitting")
          doSubmit(value, internalId)
        case JsError(errors) =>
          Logger.error("Problem transforming data for no change submission " + errors.toString())
          Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
      }
    }
  }

  private def doSubmit(value: JsValue, internalId: String)(implicit hc: HeaderCarrier): Future[VariationResponse] = {
    desService.trustVariation(value) map { response =>

      Logger.info(s"[VariationService][doSubmit] variation submitted")

      auditService.audit(
        TrustAuditing.TRUST_VARIATION,
        value,
        internalId,
        Json.toJson(response)
      )

      response
    }
  }
}
