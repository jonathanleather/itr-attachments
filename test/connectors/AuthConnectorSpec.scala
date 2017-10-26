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

import auth.{Authority, Enrolment, Identifier}
import config.{MicroserviceAppConfig, WSHttp}
import helpers.AuthHelper._
import org.mockito.Mockito._
import org.mockito.Matchers
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse }

class AuthConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  object TestConnector extends AuthConnector {
    override def serviceUrl: String = "localhost"
    override def authorityUri: String = "auth/authority"
    override def http: HttpGet with HttpPost = mockHttp
  }

  implicit val hc = HeaderCarrier()
  val mockHttp = mock[HttpGet with HttpPost]
  val confidenceLevel = ConfidenceLevel.L50
  val authResponse = Json.parse(s"""{"uri":"$uri","userDetailsLink":"$userDetailsLink","confidenceLevel":$confidenceLevel}""")
  val enrolmentResponse = (key: String) => Json.parse(
    """[{"key":"IR-SA","identifiers":[{"key":"UTR","value":"12345"}],"state":"Activated"},""" +
      s"""{"key":"$key","identifiers":[{"key":"TAVCRef","value":"$tavcRef"},{"key":"Postcode","value":"$postcode"}],"state":"Activated"}]"""
  )

  "AuthConnector" should {
    "Use WSHttp" in {
      AuthConnector.http shouldBe WSHttp
    }
    "Use the auth url from config" in {
      AuthConnector.serviceUrl shouldBe MicroserviceAppConfig.authURL
    }
  }

  "AuthConnector.getCurrentAuthority" should {
    "return Some(Authority) when auth info is found" in {
      when(mockHttp.GET[HttpResponse](Matchers.eq("localhost/auth/authority"))(Matchers.any(), Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK,Some(authResponse))))
      val result = await(TestConnector.getCurrentAuthority())
      result shouldBe Some(Authority(uri,oid,userDetailsLink,confidenceLevel))
    }
    "return None when no auth info is found" in {
      when(mockHttp.GET[HttpResponse](Matchers.eq("localhost/auth/authority"))(Matchers.any(), Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND,None)))
      val result = await(TestConnector.getCurrentAuthority())
      result shouldBe None
    }
  }

  "AuthConnector.getTAVCEnrolment" should {
    "return Some(Enrolment) when a TAVC enrolment is found" in {
      when(mockHttp.GET[HttpResponse](Matchers.eq(s"localhost$uri/enrolments"))(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK,Some(enrolmentResponse("HMRC-TAVC-ORG")))))
      val result = await(TestConnector.getTAVCEnrolment(uri))
      result shouldBe Some(Enrolment("HMRC-TAVC-ORG",Seq(Identifier("TAVCRef",tavcRef),Identifier("Postcode",postcode)),"Activated"))
    }
    "return None when no TAVC enrolment is found" in {
      when(mockHttp.GET[HttpResponse](Matchers.eq(s"localhost$uri/enrolments"))(Matchers.any(), Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK,Some(enrolmentResponse("HMCE-VATDEC-ORG")))))
      val result = await(TestConnector.getTAVCEnrolment(uri))
      result shouldBe None
    }
    "return None when a status other than OK is returned" in {
      when(mockHttp.GET[HttpResponse](Matchers.eq(s"localhost$uri/enrolments"))(Matchers.any(), Matchers.any(),Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(FORBIDDEN)))
      val result = await(TestConnector.getTAVCEnrolment(uri))
      result shouldBe None
    }
  }

}
