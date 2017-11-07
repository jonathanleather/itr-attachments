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
import play.api.http.Status._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import play.api.Logger
import auth.{Authority, Enrolment}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, HttpResponse }

trait AuthConnector {

  def serviceUrl: String

  def authorityUri: String

  def http: HttpGet with HttpPost

  def getCurrentAuthority()(implicit hc: HeaderCarrier): Future[Option[Authority]] = {
    val getUrl = s"""$serviceUrl/$authorityUri"""
    http.GET[HttpResponse](getUrl).map {
      response =>
        response.status match {
          case OK => {
            val uri = (response.json \ "uri").as[String]
            val oid = uri.substring(uri.lastIndexOf("/") + 1)
            val userDetails = (response.json \ "userDetailsLink").as[String]
            val confidenceLevel = (response.json \ "confidenceLevel").as[ConfidenceLevel]
            Some(Authority(uri, oid, userDetails, confidenceLevel))
          }
          case status => None
        }
    }
  }

  def getTAVCEnrolment(uri: String)(implicit hc: HeaderCarrier): Future[Option[Enrolment]] = {
    val getUrl = s"$serviceUrl$uri/enrolments"
    http.GET[HttpResponse](getUrl).map {
      response =>
        response.status match {
          case OK => response.json.as[Seq[Enrolment]].find(_.key == "HMRC-TAVC-ORG")
          case status => None
        }
    }
  }

}

object AuthConnector extends AuthConnector {
  lazy val serviceUrl = MicroserviceAppConfig.authURL
  val authorityUri = MicroserviceAppConfig.authorityURL
  val http: HttpGet with HttpPost = WSHttp
}
