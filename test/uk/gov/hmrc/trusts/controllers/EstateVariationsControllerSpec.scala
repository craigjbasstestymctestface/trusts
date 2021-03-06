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

import java.util.UUID

import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.trusts.BaseSpec
import uk.gov.hmrc.trusts.config.AppConfig
import uk.gov.hmrc.trusts.controllers.actions.FakeIdentifierAction
import uk.gov.hmrc.trusts.exceptions._
import uk.gov.hmrc.trusts.models.variation.{EstateVariation, VariationResponse}
import uk.gov.hmrc.trusts.services.{AuditService, DesService, ValidationService}
import uk.gov.hmrc.trusts.utils.Headers

import scala.concurrent.Future

class EstateVariationsControllerSpec extends BaseSpec with BeforeAndAfterEach{

  lazy val mockDesService: DesService = mock[DesService]

  lazy val mockAuditService: AuditService = mock[AuditService]

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockConfig: AppConfig = mock[AppConfig]

  val auditService = new AuditService(mockAuditConnector, mockConfig)
  val validationService = new ValidationService()

  val responseHandler = new VariationsResponseHandler(mockAuditService)

  override def beforeEach() = {
    reset(mockDesService, mockAuditService, mockAuditConnector, mockConfig)
  }

  private def estateVariationsController = {
    val SUT = new EstateVariationsController(new FakeIdentifierAction(Organisation), mockDesService, mockAuditService, validationService, mockConfig, responseHandler)
    SUT
  }

  val tvnResponse = "XXTVN1234567890"
  val estateVariationsAuditEvent =  "EstateVariation"

  ".estateVariation" should {

    "not perform auditing" when {
      "the feature toggle is set to false" in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        when(mockConfig.auditingEnabled).thenReturn(false)
        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = new EstateVariationsController(new FakeIdentifierAction(Organisation), mockDesService, mockAuditService, validationService, mockConfig, responseHandler)

        val result = SUT.estateVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result) { _ =>

          verify(mockAuditConnector, times(0)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "perform auditing" when {
      "the feature toggle is set to true" in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        when(mockConfig.auditingEnabled).thenReturn(true)
        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = new EstateVariationsController(new FakeIdentifierAction(Organisation),mockDesService, auditService, validationService, mockConfig, responseHandler)

        val result = SUT.estateVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        whenReady(result) { _ =>

          verify(mockAuditConnector, times(1)).sendExplicitAudit[Any](any(), any())(any(), any(), any())
        }
      }
    }

    "return 200 with TVN" when {

      "individual user called the register endpoint with a valid json payload " in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.successful(VariationResponse(tvnResponse)))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val requestPayLoad = Json.parse(validEstateVariationsRequestJson)

        val SUT = estateVariationsController

        val result = SUT.estateVariation()(
          postRequestWithPayload(requestPayLoad, withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe OK

        verify(mockAuditService).audit(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq(Json.obj("tvn" -> tvnResponse))
        )(any())

        (contentAsJson(result) \ "tvn").as[String] mustBe tvnResponse

      }

    }

    "return a BadRequest" when {

      "input request fails schema validation" in {

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.estateVariation()(
          postRequestWithPayload(Json.parse(invalidEstateVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe BAD_REQUEST

        val output = contentAsJson(result)

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Provided request is invalid.")
        )(any())

        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

      }

      "input request fails business validation" in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(BadRequestException))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.estateVariation()(
          postRequestWithPayload(Json.parse(invalidTrustBusinessValidation), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe BAD_REQUEST

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "BAD_REQUEST"
        (output \ "message").as[String] mustBe "Provided request is invalid."

      }

      "invalid correlation id is provided in the headers" in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InvalidCorrelationIdException))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val request = postRequestWithPayload(Json.parse(validEstateVariationsRequestJson), withDraftId = false)

        val result = SUT.estateVariation()(request)


        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Submission has not passed validation. Invalid CorrelationId.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INVALID_CORRELATIONID"
        (output \ "message").as[String] mustBe "Submission has not passed validation. Invalid CorrelationId."

      }

    }

    "return a Conflict" when {
      "submission with same correlation id is submitted." in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(DuplicateSubmissionException))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.estateVariation()(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson), withDraftId = false)
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe CONFLICT

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Duplicate Correlation Id was submitted.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "DUPLICATE_SUBMISSION"
        (output \ "message").as[String] mustBe "Duplicate Correlation Id was submitted."

      }
    }

    "return an internal server error" when {

      "the register endpoint called and something goes wrong." in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(InternalServerErrorException("some error")))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.estateVariation()(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          any()
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (output \ "message").as[String] mustBe "Internal server error."

      }

    }

    "return service unavailable" when {
      "the des returns Service Unavailable as dependent service is down. " in {

        when(mockDesService.estateVariation(any[EstateVariation])(any[HeaderCarrier]))
          .thenReturn(Future.failed(ServiceNotAvailableException("dependent service is down")))

        when(mockConfig.variationsApiSchema).thenReturn(appConfig.variationsApiSchema)

        val SUT = estateVariationsController

        val result = SUT.estateVariation()(
          postRequestWithPayload(Json.parse(validEstateVariationsRequestJson))
            .withHeaders(Headers.CORRELATION_HEADER -> UUID.randomUUID().toString)
        )

        status(result) mustBe SERVICE_UNAVAILABLE

        verify(mockAuditService).auditErrorResponse(
          Meq(estateVariationsAuditEvent),
          any(),
          Meq("id"),
          Meq("Service unavailable.")
        )(any())

        val output = contentAsJson(result)

        (output \ "code").as[String] mustBe "SERVICE_UNAVAILABLE"
        (output \ "message").as[String] mustBe "Service unavailable."

      }
    }
  }
}
