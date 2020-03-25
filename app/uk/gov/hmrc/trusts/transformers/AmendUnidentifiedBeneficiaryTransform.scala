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

package uk.gov.hmrc.trusts.transformers

import play.api.libs.json._
import uk.gov.hmrc.trusts.models.variation.UnidentifiedType
import uk.gov.hmrc.trusts.transformers.beneficiaries.AmendBeneficiariesCommon

case class AmendUnidentifiedBeneficiaryTransform(index: Int, description: String)
  extends DeltaTransform
  with AmendBeneficiariesCommon
  with JsonOperations {

  import uk.gov.hmrc.trusts.models.variation.UnidentifiedType._

  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    val unidentifiedBeneficiaryPath = __ \ 'details \ 'trust \ 'entities \ 'beneficiary \ 'unidentified

    val amended = getBeneficiary[UnidentifiedType](input, index, "unidentified")
      .copy(description = description)

    amendAtPosition(input, unidentifiedBeneficiaryPath, index, Json.toJson(amended))
  }
}



object AmendUnidentifiedBeneficiaryTransform {

  val key = "AmendUnidentifiedBeneficiaryTransform"

  implicit val format: Format[AmendUnidentifiedBeneficiaryTransform] = Json.format[AmendUnidentifiedBeneficiaryTransform]
}