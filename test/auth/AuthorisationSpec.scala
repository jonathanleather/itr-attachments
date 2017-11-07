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

package auth

import helpers.AuthHelper._
import connectors.AuthConnector
import org.mockito.Matchers
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._
import org.mockito.Mockito._
import play.api.mvc.Results
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class AuthorisationSpec extends UnitSpec with MockitoSugar with BeforeAndAfter {

  private def authorised() = TestAuthorisation.authorised {
    case Authorised => Future.successful(Results.Ok)
    case NotAuthorised => Future.successful(Results.Forbidden)
  }

  object TestAuthorisation extends Authorisation {
    override val authConnector: AuthConnector = mockAuthConnector
  }

  before {
    reset(mockAuthConnector)
  }

  "Authorisation.authorised" should {

    "Return an Authorised result when the user has a TAVC account with status Activated and confidenceLevel 50" in {
      setup()
      val result = authorised()
      status(result) shouldBe OK
    }

    "Return an Authorised result when the user has a TAVC account with status Activated and confidenceLevel 100" in {
      setup("Activated", ConfidenceLevel.L100)
      val result = authorised()
      status(result) shouldBe OK
    }

    "Return an Authorised result when the user has a TAVC account with status Activated and confidenceLevel 200" in {
      setup("Activated", ConfidenceLevel.L200)
      val result = authorised()
      status(result) shouldBe OK
    }

    "Return an Authorised result when the user has a TAVC account with status Activated and confidenceLevel 300" in {
      setup("Activated", ConfidenceLevel.L300)
      val result = authorised()
      status(result) shouldBe OK
    }

    "Return an Authorised result when the user has a TAVC account with status Activated and confidenceLevel 500" in {
      setup("Activated", ConfidenceLevel.L500)
      val result = authorised()
      status(result) shouldBe OK
    }

    "Return a NotAuthorised result when the user has a TAVC account with status Activated and confidenceLevel 0" in {
      setup("Activated", ConfidenceLevel.L0)
      val result = authorised()
      status(result) shouldBe FORBIDDEN
    }

    "Return a NotAuthorised result when the user has a TAVC account with status NotYetActivated and confidenceLevel 50" in {
      setup("NotYetActivated", ConfidenceLevel.L50)
      val result = authorised()
      status(result) shouldBe FORBIDDEN
    }

    "Return a NotAuthorised result when the user has a TAVC account with status Pending and confidenceLevel 50" in {
      setup("Pending", ConfidenceLevel.L50)
      val result = authorised()
      status(result) shouldBe FORBIDDEN
    }

    "Return a NotAuthorised result when the user has a TAVC account with status HandedToAgent and confidenceLevel 50" in {
      setup("HandedToAgent", ConfidenceLevel.L50)
      val result = authorised()
      status(result) shouldBe FORBIDDEN
    }

    "Return a NotAuthorised result when the user has a TAVC account with an unexpected status and confidenceLevel 50" in {
      setup("test", ConfidenceLevel.L50)
      val result = authorised()
      status(result) shouldBe FORBIDDEN
    }

    "Return a NotAuthorised result when no authority is found" in {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any[HeaderCarrier]())).thenReturn(Future.successful(None))
      when(mockAuthConnector.getTAVCEnrolment(Matchers.anyString())(Matchers.any[HeaderCarrier]())).thenReturn(Future.successful(None))
      val result = authorised()
      status(result) shouldBe FORBIDDEN
    }

  }

}
