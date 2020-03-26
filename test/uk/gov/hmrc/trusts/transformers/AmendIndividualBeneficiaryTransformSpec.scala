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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.trusts.models.{NameType, PassportType}
import uk.gov.hmrc.trusts.models.variation.{IdentificationType, IndividualDetailsType}
import uk.gov.hmrc.trusts.utils.JsonUtils

class AmendIndividualBeneficiaryTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  "AmendIndividualBeneficiaryTransform should" - {

    "before declaration" - {

      "amend a beneficiaries details by replacing the beneficiary" in {

        val beforeJson = JsonUtils.getJsonValueFromFile("trusts-individual-beneficiary-transform-before.json")
        val afterJson = JsonUtils.getJsonValueFromFile("trusts-individual-beneficiary-transform-after.json")

        val amended = IndividualDetailsType(
          lineNo = Some("2"),
          bpMatchStatus = Some("01"),
          NameType("First 2", None, "Last 2"),
          None,
          vulnerableBeneficiary = false,
          None,
          None,
          None,
          identification = Some(IdentificationType(
            nino = None,
            passport = Some(PassportType(
              number = "123456789",
              expirationDate = DateTime.parse("2025-01-01"),
              countryOfIssue = "DE"
            )),
            address = None,
            safeId = None
          )),
          DateTime.parse("2010-01-01"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo": "2",
            |  "bpMatchStatus": "01",
            |  "name": {
            |    "firstName": "First 2",
            |    "lastName": "Last 2"
            |  },
            |  "dateOfBirth": "2010-05-03",
            |  "vulnerableBeneficiary": true,
            |  "identification": {
            |    "nino": "JP1212122A"
            |  },
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendIndividualBeneficiaryTransform(1, amended, original, LocalDate.parse("2020-03-25"))

        val result = transformer.applyTransform(beforeJson).get
        result mustBe afterJson
      }
    }

    "at declaration time" - {

      "set an end date for the original beneficiary, adding in the amendment as a new beneficiary for a beneficiary known by etmp" in {

        val beforeJson =
          JsonUtils.getJsonValueFromFile("trusts-individual-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("trusts-individual-beneficiary-transform-after-declaration.json")

        val amended = IndividualDetailsType(
          lineNo = Some("2"),
          bpMatchStatus = Some("01"),
          NameType("Updated First 2", None, "Updated Last 2"),
          dateOfBirth = Some(DateTime.parse("2012-01-01")),
          vulnerableBeneficiary = false,
          None,
          None,
          None,
          identification = Some(IdentificationType(
            nino = None,
            passport = Some(PassportType(
              number = "123456789",
              expirationDate = DateTime.parse("2025-01-01"),
              countryOfIssue = "DE"
            )),
            address = None,
            safeId = None
          )),
          DateTime.parse("2010-01-01"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "lineNo": "2",
            |  "bpMatchStatus": "01",
            |  "name": {
            |    "firstName": "First 2",
            |    "lastName": "Last 2"
            |  },
            |  "dateOfBirth": "2010-05-03",
            |  "vulnerableBeneficiary": true,
            |  "identification": {
            |    "nino": "JP1212122A"
            |  },
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendIndividualBeneficiaryTransform(1, amended, original, endDate = LocalDate.parse("2020-03-25"))

        val result = transformer.applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }

      "amend the new beneficiary that is not known to etmp" in {
        val beforeJson =
          JsonUtils.getJsonValueFromFile("trusts-new-individual-beneficiary-transform-before.json")

        val afterJson =
          JsonUtils.getJsonValueFromFile("trusts-new-individual-beneficiary-transform-after-declaration.json")

        val amended = IndividualDetailsType(
          lineNo = None,
          bpMatchStatus = None,
          NameType("Amended New First 3", None, "Amended New Last 3"),
          dateOfBirth = None,
          vulnerableBeneficiary = true,
          None,
          None,
          None,
          None,
          DateTime.parse("2018-02-28"),
          None
        )

        val original: JsValue = Json.parse(
          """
            |{
            |  "name": {
            |    "firstName": "New First 3",
            |    "lastName": "New Last 3"
            |  },
            |  "vulnerableBeneficiary": false,
            |  "entityStart": "2018-02-28"
            |}
            |""".stripMargin)

        val transformer = AmendIndividualBeneficiaryTransform(2, amended, original, endDate = LocalDate.parse("2020-03-25"))

        val result = transformer.applyDeclarationTransform(beforeJson).get
        result mustBe afterJson
      }

    }

  }

}
