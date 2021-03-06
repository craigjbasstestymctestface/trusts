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
import uk.gov.hmrc.trusts.models.NameType
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustTrusteeIndividualType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddTrusteeIndTransformSpec extends FreeSpec with MustMatchers {

  "the add trustee transformer should" - {

    "add a new individual trustee when there are no trustees existing" in {
      val t = DisplayTrustTrusteeIndividualType(None,
        None,
        NameType("New", None, "Trustee"),
        Some(LocalDate.parse("2000-01-01")),
        Some("phoneNumber"),
        Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
        LocalDate.parse("1990-10-10")
      )

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-no-trustees.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-trustees-after-add.json")

      val transformer = new AddTrusteeIndTransform(t)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new individual trustee" in {

      val t = DisplayTrustTrusteeIndividualType(None,
        None,
        NameType("New", None, "Trustee"),
        Some(LocalDate.parse("2000-01-01")),
        Some("phoneNumber"),
        Some(DisplayTrustIdentificationType(None, Some("nino"), None, None)),
        LocalDate.parse("1990-10-10")
      )

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-add-ind-trustee.json")

      val transformer = new AddTrusteeIndTransform(t)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}