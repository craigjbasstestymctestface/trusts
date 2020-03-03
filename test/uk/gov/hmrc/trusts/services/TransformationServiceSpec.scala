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

package uk.gov.hmrc.trusts.services

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.{JsResult, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.trusts.models.get_trust_or_estate.get_trust._
import uk.gov.hmrc.trusts.models.{NameType, RemoveTrustee}
import uk.gov.hmrc.trusts.repositories.TransformationRepositoryImpl
import uk.gov.hmrc.trusts.transformers.{AddTrusteeIndTransform, AmendLeadTrusteeIndTransform, ComposedDeltaTransform, RemoveTrusteeTransform}
import uk.gov.hmrc.trusts.utils.JsonUtils

import scala.concurrent.Future

class TransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers {
  // Removing the usage of GuiceOneAppPerSuite started timing out a test without something like this.
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(250, Millis), interval = Span(15, Millis))

  val newLeadTrusteeIndInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = Some(DateTime.parse("2012-03-14"))
  )

  val newTrusteeIndInfo = DisplayTrustTrusteeIndividualType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = Some(new DateTime(1965, 2, 10, 12, 30)),
    phoneNumber = Some("newPhone"),
    identification = Some(DisplayTrustIdentificationType(None, Some("newNino"), None, None)),
    entityStart = DateTime.parse("2012-03-14")
  )

  val existingLeadTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = Some(DateTime.parse("2002-03-14"))
  )

  val unitTestTrusteeInfo = DisplayTrustLeadTrusteeIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
    dateOfBirth = new DateTime(1965, 2, 10, 12, 30),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = DisplayTrustIdentificationType(None, Some("newNino"), None, None),
    entityStart = Some(DateTime.parse("2012-03-14"))
  )

  val existingTrusteeIndividualInfo = DisplayTrustTrusteeIndividualType(
    lineNo = Some("1"),
    bpMatchStatus = Some("01"),
    name = NameType("John", Some("William"), "O'Connor"),
    dateOfBirth = Some(DateTime.parse("1956-02-12")),
    phoneNumber = Some("0121546546"),
    identification = Some(DisplayTrustIdentificationType(None, Some("ST123456"), None, None)),
    entityStart = DateTime.parse("1998-02-12")
  )

  private val auditService = mock[FakeAuditService]

  private implicit val hc : HeaderCarrier = HeaderCarrier()

  private val originalJson = Json.obj()

  "the transformation service" - {

    "must write an amend lead trustee transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))

      }
    }

    "must write an add trustee ind transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAddTrusteeTransformer("utr", "internalId", DisplayTrustTrusteeType(Some(newTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AddTrusteeIndTransform(newTrusteeIndInfo))))

      }
    }

    "must write a RemoveTrustee transform to the transformation repository with no existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = DateTime.parse("2010-10-10")

      val payload = RemoveTrustee(
        endDate = endDate,
        index = 0
      )

      val result = service.addRemoveTrusteeTransformer("utr", "internalId", payload)

      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(RemoveTrusteeTransform(endDate, index = 0, originalJson))))
      }
    }

    "must write a RemoveTrustee transform to the transformation repository with existing transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      val existingTransforms = Seq(AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val endDate = DateTime.parse("2010-10-10")

      val payload = RemoveTrustee(
        endDate = endDate,
        index = 0
      )

      val result = service.addRemoveTrusteeTransformer("utr", "internalId", payload)

      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
            RemoveTrusteeTransform(endDate, index = 0, originalJson)
          )))
      }
    }

    "must write a corresponding transform to the transformation repository with existing empty transforms" in {

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))

      }
    }

    "must write a corresponding transform to the transformation repository with existing transforms" in {

      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      val existingTransforms = Seq(AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendLeadTrusteeTransformer("utr", "internalId", DisplayTrustLeadTrusteeType(Some(newLeadTrusteeIndInfo), None))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
            AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo))))

      }
    }
    "must transform json data with the current transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      val existingTransforms = Seq(
        AmendLeadTrusteeIndTransform(existingLeadTrusteeInfo),
        AmendLeadTrusteeIndTransform(newLeadTrusteeIndInfo),
        AmendLeadTrusteeIndTransform(unitTestTrusteeInfo)
      )
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")
      val afterJson: JsValue = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-after-ind.json")

      val result: Future[JsResult[JsValue]] = service.applyTransformations("utr", "internalId", beforeJson)

      whenReady(result) {
        r => r.get mustEqual afterJson
      }
    }
    "must transform json data when no current transforms" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(None))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-transform-before.json")

      val result: Future[JsResult[JsValue]] = service.applyTransformations("utr", "internalId", beforeJson)

      whenReady(result) {
        r => r.get mustEqual beforeJson
      }
    }
    "must apply the correspondence address to the lead trustee's address if it doesn't have one" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository, auditService)

      val beforeJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address.json")
      val afterJson = JsonUtils.getJsonValueFromFile("trusts-lead-trustee-and-correspondence-address-after.json")

      val result: JsResult[JsValue] = service.populateLeadTrusteeAddress(beforeJson)

      result.get mustEqual afterJson
    }
  }
}
