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

import play.api.libs.json.{JsValue, Json}

import scala.io.Source


trait JsonUtils {

  def getJsonFromFile(filename :String) :String = {
    val jsonString = Source.fromFile(getClass.getResource(s"/$filename").getPath).mkString
    jsonString
  }

  def getJsonValueFromFile(filename:String) :JsValue = {
    Json.parse(getJsonFromFile(filename))
  }

}

object JsonUtils extends JsonUtils