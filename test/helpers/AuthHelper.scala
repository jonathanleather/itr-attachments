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

package helpers

import auth.{Authority, Enrolment, Identifier}
import connectors.AuthConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

object AuthHelper extends MockitoSugar {
  implicit val hc = HeaderCarrier()
  val mockAuthConnector = mock[AuthConnector]
  val oid = "1234567890"
  val uri = s"auth/oid/$oid"
  val tavcRef = "AA1234567890000"
  val postcode = "AA1 1AA"
  val userDetailsLink = s"localhost/user-details/id/$oid"
  val authority = (confidenceLevel: ConfidenceLevel) => Some(Authority(uri,oid,userDetailsLink,confidenceLevel))
  val enrolment = (status: String) => Some(Enrolment("HMRC-TAVC-ORG",Seq(Identifier("TAVCRef",tavcRef),Identifier("Postcode",postcode)),status))


  def setup(status: String = "Activated", confidenceLevel: ConfidenceLevel = ConfidenceLevel.L50): Unit = {
    when(mockAuthConnector.getCurrentAuthority()(Matchers.any[HeaderCarrier]())).thenReturn(Future.successful(authority(confidenceLevel)))
    when(mockAuthConnector.getTAVCEnrolment(Matchers.anyString())(Matchers.any[HeaderCarrier]())).thenReturn(Future.successful(enrolment(status)))
  }

}
