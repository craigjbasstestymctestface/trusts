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
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust.{DisplayTrustIdentificationType, DisplayTrustProtector}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AddIndividualProtectorTransformSpec extends FreeSpec with MustMatchers {

  val newProtector = DisplayTrustProtector(Some("1"),
    None,
    NameType("abcdefghijkl",Some("abcdefghijklmn"), "abcde"),
    Some(LocalDate.parse("2000-01-01")),
    Some(DisplayTrustIdentificationType(None,Some("ST019091"),None,None)),
    LocalDate.parse("2002-01-01")
  )

  val newSecondProtector = DisplayTrustProtector(None,
    None,
    NameType("second",None, "protector"),
    Some(LocalDate.parse("2000-01-01")),
    Some(DisplayTrustIdentificationType(None,Some("AB123456"),None,None)),
    LocalDate.parse("2010-01-01")
  )

  "the add individual protector transformer should" - {

    "add a new individual protector when there are no protector existing" in {
      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-cached-no-protectors.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-protector.json")

      val transformer = new AddIndividualProtectorTransform(newProtector)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

    "add a new individual protector" in {

      val trustJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-individual-protector.json")

      val afterJson = JsonUtils.getJsonValueFromFile("trusts-etmp-get-trust-after-add-second-individual-protector.json")

      val transformer = new AddIndividualProtectorTransform(newSecondProtector)

      val result = transformer.applyTransform(trustJson).get

      result mustBe afterJson
    }

  }
}