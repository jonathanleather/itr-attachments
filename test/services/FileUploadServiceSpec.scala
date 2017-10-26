/*
 * Copyright 2017 HM Revenue & Customs
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

package services

import connectors.FileUploadConnector
import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, Upstream5xxResponse }

class FileUploadServiceSpec extends UnitSpec with MockitoSugar with WithFakeApplication with BeforeAndAfter {

  val mockFileUploadConnector = mock[FileUploadConnector]
  val envelopeID = "00000000-0000-0000-0000-000000000000"
  val fileID = "1"
  implicit val hc = HeaderCarrier()

  val envelopeStatusResponse = Json.parse("""{
    |  "id": "00000000-0000-0000-0000-000000000000",
    |  "callbackUrl": "http://absolute.callback.url",
    |  "metadata": {
    |  },
    |  "status": "OPEN"
    |}""".stripMargin)

  before{
    reset(mockFileUploadConnector)
  }

  object TestService extends FileUploadService {
    override lazy val fileUploadConnector = mockFileUploadConnector
  }

  "FileUploadService" should {

    "Use the correct FileUploadConnector" in {
      FileUploadService.fileUploadConnector shouldBe FileUploadConnector
    }

  }

  "createEnvelope" when {

    "connector returns OK" should {

      lazy val result = TestService.createEnvelope

      "return an envelope ID from the location header" in {
        when(mockFileUploadConnector.createEnvelope()(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK,
            responseHeaders = Map("Location" -> Seq(s"file-upload/file-upload/envelopes/$envelopeID")))))
        await(result) shouldBe envelopeID
      }
    }

    "connector returns non-OK" should {

      lazy val result = TestService.createEnvelope

      "return an empty string" in {
        when(mockFileUploadConnector.createEnvelope()(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        await(result) shouldBe ""
      }

    }

    "connector returns failed future" should {

      lazy val result = TestService.createEnvelope

      "return an empty string" in {
        when(mockFileUploadConnector.createEnvelope()(Matchers.any()))
          .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
        await(result) shouldBe ""
      }

    }
  }

  "getEnvelopeStatus" when {

    "connector returns OK" should {

      lazy val result = TestService.getEnvelopeStatus(envelopeID)

      "return the response from the connector" in {
        when(mockFileUploadConnector.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(envelopeStatusResponse))))
        await(result).status shouldBe OK
        await(result).json shouldBe envelopeStatusResponse
      }

    }

    "connector returns non-OK" should {

      lazy val result = TestService.getEnvelopeStatus(envelopeID)

      "return the response from the connector" in {
        when(mockFileUploadConnector.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        await(result).status shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "connector returns failed future" should {

      lazy val result = TestService.getEnvelopeStatus(envelopeID)

      "return an INTERNAL_SERVER_ERROR" in {
        when(mockFileUploadConnector.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any()))
          .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
        await(result).status shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "closeEnvelope" when {

    "connector returns CREATED" should {

      lazy val result = TestService.closeEnvelope(envelopeID)

      "return the response from the connector" in {
        when(mockFileUploadConnector.closeEnvelope(Matchers.eq(envelopeID))(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED)))
        await(result).status shouldBe CREATED
      }

    }

    "connector returns non-CREATED" should {

      lazy val result = TestService.closeEnvelope(envelopeID)

      "return the response from the connector" in {
        when(mockFileUploadConnector.closeEnvelope(Matchers.eq(envelopeID))(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        await(result).status shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "connector returns failed future" should {

      lazy val result = TestService.closeEnvelope(envelopeID)

      "return an INTERNAL_SERVER_ERROR" in {
        when(mockFileUploadConnector.closeEnvelope(Matchers.eq(envelopeID))(Matchers.any()))
          .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
        await(result).status shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "deleteFile" when {

    "connector returns OK" should {

      lazy val result = TestService.deleteFile(envelopeID, fileID)

      "return the response from the connector" in {
        when(mockFileUploadConnector.deleteFile(Matchers.eq(envelopeID),Matchers.eq(fileID))(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        await(result).status shouldBe OK
      }

    }

    "connector returns non-OK" should {

      lazy val result = TestService.deleteFile(envelopeID, fileID)

      "return the response from the connector" in {
        when(mockFileUploadConnector.deleteFile(Matchers.eq(envelopeID),Matchers.eq(fileID))(Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        await(result).status shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "connector returns failed future" should {

      lazy val result = TestService.deleteFile(envelopeID, fileID)

      "return an INTERNAL_SERVER_ERROR" in {
        when(mockFileUploadConnector.deleteFile(Matchers.eq(envelopeID),Matchers.eq(fileID))(Matchers.any()))
          .thenReturn(Future.failed(Upstream5xxResponse("", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))
        await(result).status shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
