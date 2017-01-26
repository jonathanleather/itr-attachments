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

import scala.concurrent.Future

object FileUploadConnector extends FileUploadConnector {
  override lazy val http = WSHttp
  override lazy val serviceURL = MicroserviceAppConfig.fileUploadURL
}

trait FileUploadConnector {

  val http: HttpGet with HttpPost with HttpPut with HttpDelete
  val serviceURL: String

  val createEnvelopeJSON = Json.parse("""{
                                        |  "constraints" : {
                                        |    "contentTypes" : [
                                        |        "application/pdf"
                                        |    ],
                                        |    "maxItems" : 5,
                                        |    "maxSizePerItem" : "5MB"
                                        |  }
                                        |}""".stripMargin)

  val closeEnvelopeJSON = (envelopeId: String) => Json.parse(s"""{
                                         |   "envelopeId" : "$envelopeId",
                                         |   "application" : "tavc",
                                         |   "destination" : "DMS"
                                         |}""".stripMargin)

  def createEnvelope()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST(s"$serviceURL/file-upload/envelopes", createEnvelopeJSON)
  }

  def getEnvelopeStatus(envelopeId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.GET(s"$serviceURL/file-upload/envelopes/$envelopeId")
  }

  def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    println("========================In close envelop connector. env id is")
    println(envelopeId)
    http.POST(s"$serviceURL/file-routing/requests", closeEnvelopeJSON(envelopeId))
  }

  def deleteFile(envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE(s"$serviceURL/file-upload/envelopes/$envelopeId/files/$fileId")
  }

}
