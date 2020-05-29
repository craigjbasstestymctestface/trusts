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

package uk.gov.hmrc.trusts.controllers

import java.time.{LocalDate, LocalDateTime}

import akka.util.ByteString
import org.mockito.Matchers.any
import org.scalatest._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.{Action, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.models.{RegistrationSubmissionDraft, Success}
import uk.gov.hmrc.trusts.repositories.RegistrationSubmissionRepository
import uk.gov.hmrc.trusts.services.{AuditService, LocalDateService, LocalDateTimeService}
import uk.gov.hmrc.trusts.utils.JsonRequests

import scala.concurrent.Future

class SubmissionDraftControllerSpec extends WordSpec with MockitoSugar with MustMatchers with JsonRequests with Inside with ScalaFutures {

  private val currentDateTime: LocalDateTime = LocalDateTime.of(1999, 3, 14, 13, 33)
  private object LocalDateTimeServiceStub extends LocalDateTimeService {
    override def now: LocalDateTime = currentDateTime
  }

  private val existingDraftData = Json.parse(
    """
      |{
      | "anotherKey": {
      |   "foo": "bar",
      |   "fizzbinn": true
      | },
      | "sectionKey": {
      |   "field1": "value1",
      |   "field2": "value2",
      |   "field3": 3
      | }
      |}
      |""".stripMargin
  )

  private val existingDraft = RegistrationSubmissionDraft(
    "DRAFTID",
    "id",
    LocalDateTime.of(1997, 3, 14, 14, 45),
    existingDraftData
  )

  ".setSection" should {
    "cause creation of draft with section if none exists" in {
      val identifierAction = new FakeIdentifierAction(Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]
      val auditService = mock[AuditService]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        auditService,
        LocalDateTimeServiceStub
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(None))

      when(submissionRepository.setDraft(any()))
        .thenReturn(Future.successful(true))

      val body = Json.parse(
        """
          |{
          | "field1": "value1",
          | "field2": "value2",
          | "field3": 3
          |}
          |""".stripMargin)

      val request = FakeRequest("POST", "path")
        .withBody(body)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val draftData  = Json.parse(
        """
          |{
          | "sectionKey": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft("DRAFTID", "id", currentDateTime, draftData)

      val result = controller.setSection("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
      verify(submissionRepository).setDraft(expectedDraft)
    }

    "modify existing draft if one exists" in {
      val identifierAction = new FakeIdentifierAction(Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]
      val auditService = mock[AuditService]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        auditService,
        LocalDateTimeServiceStub
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      when(submissionRepository.setDraft(any()))
        .thenReturn(Future.successful(true))

      val body = Json.parse(
        """
          |{
          | "field1": "value1",
          | "field2": "value2",
          | "field3": 3
          |}
          |""".stripMargin)

      val request = FakeRequest("POST", "path")
        .withBody(body)
        .withHeaders(CONTENT_TYPE -> "application/json")

      val draftData  = Json.parse(
        """
          |{
          | "anotherKey": {
          |   "foo": "bar",
          |   "fizzbinn": true
          | },
          | "sectionKey": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | }
          |}
          |""".stripMargin)

      val expectedDraft = RegistrationSubmissionDraft("DRAFTID", "id", existingDraft.createdAt, draftData)

      val result = controller.setSection("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      verify(submissionRepository).getDraft("DRAFTID", "id")
      verify(submissionRepository).setDraft(expectedDraft)
    }
    "get existing draft when one exists" in {
      val identifierAction = new FakeIdentifierAction(Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]
      val auditService = mock[AuditService]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        auditService,
        LocalDateTimeServiceStub
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getSection("DRAFTID", "sectionKey").apply(request)
      status(result) mustBe OK

      val expectedDraftJson = Json.parse(
        """
          |{
          | "createdAt": "1997-03-14T14:45:00",
          | "data": {
          |   "field1": "value1",
          |   "field2": "value2",
          |   "field3": 3
          | }
          |}
          |""".stripMargin)

      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe expectedDraftJson
    }
    "return not found when none exists in draft" in {
      val identifierAction = new FakeIdentifierAction(Organisation)
      val submissionRepository = mock[RegistrationSubmissionRepository]
      val auditService = mock[AuditService]

      val controller = new SubmissionDraftController(
        submissionRepository,
        identifierAction,
        auditService,
        LocalDateTimeServiceStub
      )

      when(submissionRepository.getDraft(any(), any()))
        .thenReturn(Future.successful(Some(existingDraft)))

      val request = FakeRequest("GET", "path")

      val result = controller.getSection("DRAFTID", "sectionKey2").apply(request)
      status(result) mustBe NO_CONTENT
    }
  }
  "return not found when there is no draft" in {
    val identifierAction = new FakeIdentifierAction(Organisation)
    val submissionRepository = mock[RegistrationSubmissionRepository]
    val auditService = mock[AuditService]

    val controller = new SubmissionDraftController(
      submissionRepository,
      identifierAction,
      auditService,
      LocalDateTimeServiceStub
    )

    when(submissionRepository.getDraft(any(), any()))
      .thenReturn(Future.successful(None))

    val request = FakeRequest("GET", "path")

    val result = controller.getSection("DRAFTID", "sectionKey2").apply(request)
    status(result) mustBe NOT_FOUND
  }
  "get all drafts when some exist" in {
    val identifierAction = new FakeIdentifierAction(Organisation)
    val submissionRepository = mock[RegistrationSubmissionRepository]
    val auditService = mock[AuditService]

    val controller = new SubmissionDraftController(
      submissionRepository,
      identifierAction,
      auditService,
      LocalDateTimeServiceStub
    )

    val drafts = List(
      RegistrationSubmissionDraft("draftId1", "id", LocalDateTime.of(2012, 2, 3, 9, 30), Json.obj()),
      RegistrationSubmissionDraft("draftId2", "id", LocalDateTime.of(2010, 10, 10, 14, 40), Json.obj())
    )

    when(submissionRepository.getAllDrafts(any()))
      .thenReturn(Future.successful(drafts))

    val request = FakeRequest("GET", "path")

    val result = controller.getDrafts().apply(request)
    status(result) mustBe OK

    verify(submissionRepository).getAllDrafts("id")

    val expectedDraftJson = Json.parse(
      """
        |[
        | {
        |   "createdAt": "2012-02-03T09:30:00",
        |   "draftId": "draftId1"
        | },
        | {
        |   "createdAt": "2010-10-10T14:40:00",
        |   "draftId": "draftId2"
        | }
        |]
        |""".stripMargin)

    contentType(result) mustBe Some(JSON)
    contentAsJson(result) mustBe expectedDraftJson
  }
  "get all drafts when none exist" in {
    val identifierAction = new FakeIdentifierAction(Organisation)
    val submissionRepository = mock[RegistrationSubmissionRepository]
    val auditService = mock[AuditService]

    val controller = new SubmissionDraftController(
      submissionRepository,
      identifierAction,
      auditService,
      LocalDateTimeServiceStub
    )

    when(submissionRepository.getAllDrafts(any()))
      .thenReturn(Future.successful(List()))

    val request = FakeRequest("GET", "path")

    val result = controller.getDrafts().apply(request)
    status(result) mustBe OK

    verify(submissionRepository).getAllDrafts("id")

    val expectedDraftJson = Json.parse("[]")

    contentType(result) mustBe Some(JSON)
    contentAsJson(result) mustBe expectedDraftJson
  }
  "remove draft" in {
    val identifierAction = new FakeIdentifierAction(Organisation)
    val submissionRepository = mock[RegistrationSubmissionRepository]
    val auditService = mock[AuditService]

    val controller = new SubmissionDraftController(
      submissionRepository,
      identifierAction,
      auditService,
      LocalDateTimeServiceStub
    )

    when(submissionRepository.removeDraft(any(), any()))
      .thenReturn(Future.successful(true))

    val request = FakeRequest("GET", "path")

    val result = controller.removeDraft("DRAFTID").apply(request)
    status(result) mustBe OK

    verify(submissionRepository).removeDraft("DRAFTID", "id")
  }
}