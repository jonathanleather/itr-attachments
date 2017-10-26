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

package connectors
import config.{MicroserviceAppConfig, WSHttp}
import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.ws.WSHttp
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse, HttpDelete }
import scala.concurrent.Future
import uk.gov.hmrc.http._

class FileUploadConnectorSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  object TestConnector extends FileUploadConnector {
    override def http = mock[HttpPost with HttpDelete with HttpPut with HttpGet]
    override lazy val serviceURL = "file-upload"
  }

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



  "FileUploadConnector" should {

    "Use WSHttp" in {
      FileUploadConnector.http shouldBe WSHttp
    }

    "Determine the service url from app config" in {
      FileUploadConnector.serviceURL shouldBe MicroserviceAppConfig.fileUploadURL
    }

  }

  "createEnvelope" should {

    lazy val result = TestConnector.createEnvelope()

    "Send the JSON defined in createEnvelopeJSON as the body" in {
      when(TestConnector.http.POST[JsValue,HttpResponse](Matchers.eq(s"${TestConnector.serviceURL}/file-upload/envelopes"),
        Matchers.eq(TestConnector.createEnvelopeJSON),Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, responseHeaders = Map("Location" -> Seq(s"file-upload/file-upload/envelopes/$envelopeID")))))
      await(result).status shouldBe OK
    }

  }

  "getEnvelopeStatus" should {

    lazy val result = TestConnector.getEnvelopeStatus(envelopeID)

    "Send the envelopeID in the URL" in {
      when(TestConnector.http.GET[HttpResponse](Matchers.eq(s"${TestConnector.serviceURL}/file-upload/envelopes/$envelopeID"))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(envelopeStatusResponse))))
      await(result).status shouldBe OK
      await(result).json shouldBe envelopeStatusResponse
    }

  }

  "closeEnvelope" should {

    lazy val result = TestConnector.closeEnvelope(envelopeID)

    "Send the JSON defined in closeEnvelopeJSON as the body" in {
      when(TestConnector.http.POST[JsValue,HttpResponse](Matchers.eq(s"${TestConnector.serviceURL}/file-routing/requests"),
        Matchers.eq(TestConnector.closeEnvelopeJSON(envelopeID)),Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, responseHeaders = Map("Location" -> Seq(s"/file-routing/requests/$envelopeID")))))
      await(result).status shouldBe OK
    }

  }

  "deleteFile" should {

    lazy val result = TestConnector.deleteFile(envelopeID, fileID)

    "Send the envelopeID and fileID in the URL" in {
      when(TestConnector.http.DELETE[HttpResponse](Matchers.eq(s"${TestConnector.serviceURL}/file-upload/envelopes/$envelopeID/files/$fileID"))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
      await(result).status shouldBe OK
    }

  }

}
