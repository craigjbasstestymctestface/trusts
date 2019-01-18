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

package uk.gov.hmrc.trusts.utils

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.trusts.connectors.BaseSpec
import uk.gov.hmrc.trusts.models.Registration


class DesSchemaValidatorSpec extends BaseSpec with  GuiceOneServerPerSuite {

  "DesSchemaValidator" should {
    "return successvalidation when json string is valid " when {
      "Json having all required fields" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-trusts-registration.json")
        val validationResult = DesSchemaValidator.validateRegistrationAgainstSchema(jsonString)
        validationResult.isSuccess mustBe true
        println(Json.parse(jsonString).validate[Registration])
        Json.parse(jsonString).validate[Registration].fold (
           errors => println("error"),
           request => println(request)
        )
        Json.parse(jsonString).validate[Registration].isSuccess mustBe true
      }
  }
  }

}
