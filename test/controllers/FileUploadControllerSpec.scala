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

package controllers

import connectors.AuthConnector
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import services.FileUploadService
import helpers.AuthHelper._
import org.mockito.Matchers
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

class FileUploadControllerSpec extends UnitSpec with WithFakeApplication with BeforeAndAfter with MockitoSugar {

  val mockFileUploadService = mock[FileUploadService]
  val envelopeID = "00000000-0000-0000-0000-000000000000"
  val fileID = "1"
  val envelopeStatusResponse = Json.parse("""{
    |  "id": "00000000-0000-0000-0000-000000000000",
    |  "callbackUrl": "http://absolute.callback.url",
    |  "metadata": {
    |  },
    |  "status": "OPEN"
    |}""".stripMargin)

  object TestController extends FileUploadController {
    override lazy val authConnector = mockAuthConnector
    override lazy val fileUploadService = mockFileUploadService
  }


  before {
    reset(mockAuthConnector)
  }

  "FileUploadController" should {

    "Use the correct AuthConnector" in {
      FileUploadController.authConnector shouldBe AuthConnector
    }

  }

  "createEnvelope" when {

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and an envelope ID is returned by the file upload service" should {

      lazy val result = TestController.createEnvelope(FakeRequest())

      "return an OK" in {
        setup()
        when(mockFileUploadService.createEnvelope(Matchers.any(), Matchers.any())).thenReturn(Future.successful(envelopeID))
        status(result) shouldBe OK
      }

      "return json containing the envelope ID returned from the file upload service" in {
        setup()
        when(mockFileUploadService.createEnvelope(Matchers.any(), Matchers.any())).thenReturn(Future.successful(envelopeID))
        contentAsJson(result) shouldBe TestController.createEnvelopeResponse(envelopeID)
      }
    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and an empty string is returned by the file upload service" should {

      lazy val result = TestController.createEnvelope(FakeRequest())

      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.createEnvelope(Matchers.any(), Matchers.any())).thenReturn(Future.successful(""))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and the file upload service returns a failed future" should {

      lazy val result = TestController.createEnvelope(FakeRequest())

      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.createEnvelope(Matchers.any(), Matchers.any())).thenReturn(Future.failed(new Exception))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "calling the method with a TAVC account with status NotYetActivated and confidence level 50" should {
      "return status FORBIDDEN" in {
        setup("NotYetActivated")
        val result = TestController.createEnvelope(FakeRequest())
        status(result) shouldBe FORBIDDEN
      }
    }
  }

  "getEnvelopeStatus" when {

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and a result with status OK is returned from the file upload service" should {

      lazy val result = TestController.getEnvelopeStatus(envelopeID)(FakeRequest())

      "return an OK" in {
        setup()
        when(mockFileUploadService.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK,responseJson = Some(envelopeStatusResponse))))
        status(result) shouldBe OK
      }

      "return json containing the envelope status returned from the file upload service" in {
        setup()
        when(mockFileUploadService.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK,responseJson = Some(envelopeStatusResponse))))
        contentAsJson(result) shouldBe envelopeStatusResponse
      }
    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and a non-OK response is returned by the file upload service" should {

      lazy val result = TestController.getEnvelopeStatus(envelopeID)(FakeRequest())

      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and the file upload service returns a failed future" should {

      lazy val result = TestController.getEnvelopeStatus(envelopeID)(FakeRequest())

      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.getEnvelopeStatus(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(new Exception))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "closeEnvelope" when {

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and a result with status CREATED is returned from the file upload service" should {

      lazy val result = TestController.closeEnvelope(envelopeID)(FakeRequest())

      "return an OK" in {
        setup()
        when(mockFileUploadService.closeEnvelope(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(CREATED)))
        status(result) shouldBe OK
      }
    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and a non-CREATED response is returned by the file upload service" should {

      lazy val result = TestController.closeEnvelope(envelopeID)(FakeRequest())

      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.closeEnvelope(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and the file upload service returns a failed future" should {

      lazy val result = TestController.closeEnvelope(envelopeID)(FakeRequest())

      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.closeEnvelope(Matchers.eq(envelopeID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(new Exception))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }

  "deleteFile" when {

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and a result with status OK is returned from the file upload service" should {

      lazy val result = TestController.deleteFile(envelopeID, fileID)(FakeRequest())

      "return an OK" in {
        setup()
        when(mockFileUploadService.deleteFile(Matchers.eq(envelopeID), Matchers.eq(fileID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))
        status(result) shouldBe OK
      }
    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and a non-OK response is returned by the file upload service" should {

      lazy val result = TestController.deleteFile(envelopeID, fileID)(FakeRequest())
      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.deleteFile(Matchers.eq(envelopeID), Matchers.eq(fileID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "calling the method with a TAVC account with status Activated and confidence level 50" +
      " and the file upload service returns a failed future" should {

      lazy val result = TestController.deleteFile(envelopeID, fileID)(FakeRequest())
      "return an INTERNAL_SERVER_ERROR" in {
        setup()
        when(mockFileUploadService.deleteFile(Matchers.eq(envelopeID), Matchers.eq(fileID))(Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(new Exception))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "calling the method with a TAVC account with status NotYetActivated and confidence level 50" should {
      "return status FORBIDDEN" in {
        setup("NotYetActivated")
        val result = TestController.deleteFile(envelopeID, fileID)(FakeRequest())
        status(result) shouldBe FORBIDDEN
      }
    }
  }

}
