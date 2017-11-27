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
import play.api.libs.json.Json
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpDelete, HttpGet, HttpPost, HttpPut, HttpResponse }

object FileUploadConnector extends FileUploadConnector {
  override lazy val http = WSHttp
  override lazy val serviceURL = MicroserviceAppConfig.fileUploadURL
  override lazy val submissionUrl = MicroserviceAppConfig.submissionUrl
}

trait FileUploadConnector {

  val http: HttpGet with HttpPost with HttpPut with HttpDelete
  val serviceURL: String
  val submissionUrl: String

  val createEnvelopeJSON = Json.parse("""{
                                        |  "callbackUrl": "http://localhost:9644/investment-tax-relief-attachments/file-upload-callback/envelopes",
                                        |  "constraints" : {
                                        |    "maxSizePerItem" : "10MB"
                                        |  }
                                        |}""".stripMargin)

  val closeEnvelopeJSON = (envelopeId: String) => Json.parse(s"""{
                                         |   "envelopeId" : "$envelopeId",
                                         |   "application" : "tavc",
                                         |   "destination" : "TAVC"
                                         |}""".stripMargin)

  def createEnvelope()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST(s"$serviceURL/file-upload/envelopes", createEnvelopeJSON)
  }

  def getEnvelopeStatus(envelopeId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET(s"$serviceURL/file-upload/envelopes/$envelopeId")
  }

  def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST(s"$serviceURL/file-routing/requests", closeEnvelopeJSON(envelopeId))
  }

  def deleteFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE(s"$serviceURL/file-upload/envelopes/$envelopeId/files/$fileId")
  }

}
