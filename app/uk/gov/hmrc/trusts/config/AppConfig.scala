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

package uk.gov.hmrc.trusts.config

import javax.inject.{Inject, Singleton}

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig


@Singleton
class AppConfig @Inject()(config: Configuration, playEnv: Environment) extends ServicesConfig {

  val desUrl: String = baseUrl("des")
  val desEnvironment: String = loadConfig("microservice.services.des.environment")
  val desToken: String = loadConfig("microservice.services.des.token")

  val trustsApiRegistrationSchema: String = "/resources/schemas/trustsApiRegistrationSchema_3.1.0.json"

  override protected def mode: Mode = playEnv.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(
    throw new Exception(s"Missing configuration key : $key")
  )

  override protected def runModeConfiguration: Configuration = config

}
