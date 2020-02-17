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

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationOrgType, DisplayTrustLeadTrusteeOrgType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class SetLeadTrusteeOrgTransformSpec extends FreeSpec with MustMatchers with OptionValues {
  "the modify lead transformer should" - {

    "successfully set a new org lead trustee's details" in {
      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-after-org.json")
      val newTrusteeInfo = DisplayTrustLeadTrusteeOrgType(
        lineNo = "newLineNo",
        bpMatchStatus = Some("newMatchStatus"),
        name = "newName",
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = DisplayTrustIdentificationOrgType(None, Some("newUtr"), None),
        entityStart = "2012-03-14"
      )
      val transformer = SetLeadTrusteeOrgTransform(newTrusteeInfo)

      val result = transformer.applyTransform(beforeJson).get
      result mustBe afterJson
    }

  }
}
