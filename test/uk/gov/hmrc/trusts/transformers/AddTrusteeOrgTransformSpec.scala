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

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.DisplayTrustTrusteeOrgType
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddTrusteeOrgTransformSpec extends FreeSpec with MustMatchers {

  "the add trustee transformer should" - {

    "add a new organisation trustee when there are no trustees existing" in {
      val t = DisplayTrustTrusteeOrgType(None,
        None,
        "Company Name",
        None,
        None,
        None,
        LocalDate.parse("2020-01-30")
      )

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-no-trustees.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-trustees-after-org-add.json")

      val transformer = AddTrusteeOrgTransform(t)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new org trustee" in {

      val t = DisplayTrustTrusteeOrgType(
        None,
        None,
        "Company Name",
        None,
        None,
        None,
        LocalDate.parse("2020-01-30")
      )

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-org-trustee.json")

      val transformer = AddTrusteeOrgTransform(t)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}