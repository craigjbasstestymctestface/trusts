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

import org.joda.time.DateTime
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.trusts.models.{NameType, RemoveTrustee}
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustTrusteeIndividualType, DisplayTrustTrusteeType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class RemoveTrusteeTransformSpec extends FreeSpec with MustMatchers with OptionValues  {

  "the remove trustee transformer must" - {

    "remove an already known individual trustee" in {

      val t = DisplayTrustTrusteeIndividualType(
        lineNo = "1",
        bpMatchStatus = Some("01"),
        name = NameType("John", Some("William"), "O'Connor"),
        dateOfBirth = Some(DateTime.parse("1956-02-12")),
        phoneNumber = Some("0121546546"),
        identification = Some(DisplayTrustIdentificationType(None, Some("ST123456"), None, None)),
        entityStart = DateTime.parse("1998-02-12")
      )

      val endDate = LocalDate.of(2010, 10, 15)

      val trustee = DisplayTrustTrusteeType(
        trusteeInd = Some(t),
        trusteeOrg = None
      )

      val cachedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached.json")

      val transformedJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-remove-trustee-ind.json")

      val transformer = new RemoveTrusteeTransform(trustee, endDate)

      val result = transformer.applyTransform(cachedJson).get

      result mustBe transformedJson
    }

    "remove a newly added individual trustee" ignore {

    }

  }

}
