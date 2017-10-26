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

import org.mockito.ArgumentCaptor
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent, ExtendedDataEvent}
import uk.gov.hmrc.http.HttpResponse

object AuditHelper extends AuditHelper

trait AuditHelper {


  val testRequestPath = "test/path"
  val responseReasonContent = (message: String) => s"""{"reason" : "$message"}"""
  val responseSuccessContent = s"""{"processingDate":"2014-12-17T09:30:47Z","formBundleNumber":"FBUND98763284"}"""

  // logging
  val reasonMessage = (message: String) => s"""{"reason" : "$message"}"""
  //val eventCaptor2 = ArgumentCaptor.forClass(classOf[DataEvent])
  val eventCaptor = ArgumentCaptor.forClass(classOf[Audit])

  val responseNonContent = HttpResponse(NO_CONTENT)
  val responseBadRequestNoContent = HttpResponse(BAD_REQUEST)
  val responseNotFoundNoContent = HttpResponse(NOT_FOUND)
  val responseServiceUnavailableNoContent = HttpResponse(SERVICE_UNAVAILABLE)
  val responseInternalServerErrorNoContent = HttpResponse(INTERNAL_SERVER_ERROR)
  val responseOtherErrorNoContent = HttpResponse(GATEWAY_TIMEOUT)
  val responseOkSuccess = HttpResponse(OK, Some(Json.parse(responseSuccessContent)))
  val responseOkNocontent = HttpResponse(OK)
  val responseCreatedNocontent = HttpResponse(CREATED)
  val responseBadRequestEtmpDuplicate = HttpResponse(BAD_REQUEST,
    Some(Json.parse(responseReasonContent(EtmpResponseReasons.duplicateSubmission400))))
  val responseServiceUnavailableEtmpNotProcessed = HttpResponse(SERVICE_UNAVAILABLE,
    Some(Json.parse(responseReasonContent(EtmpResponseReasons.notProcessed503))))
  val responseInternalServerErrorEtmpSap = HttpResponse(INTERNAL_SERVER_ERROR,
    Some(Json.parse(responseReasonContent(EtmpResponseReasons.sapError500))))
  val responseInternalServerErrorEtmpRegime = HttpResponse(INTERNAL_SERVER_ERROR,
    Some(Json.parse(responseReasonContent(EtmpResponseReasons.noRegime500))))
  val responseInternalServerErrorEtmp = HttpResponse(INTERNAL_SERVER_ERROR,
    Some(Json.parse(responseReasonContent(EtmpResponseReasons.serverError500))))
  val submissionControllerTestName = "SubmissionController"
  val subscriptionControllerTestName = "SubscriptionController"
  val subscribeTestAction = "subscribe"
  val submitTestAction = "submit"

  val safeId = "XA0001234567890"
  val tavcRefNumber = "XLTAVC000823190"
  val acknowledgementReference = "XE00012345678901477052976"

}
