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

object EtmpResponseReasons {
  val success = "Success"
  val duplicateSubmission400 = "Duplicate submission acknowledgement reference from remote endpoint returned"
  val errors400 = "Your submission contains one or more errors"
  val invalidJson400 = "Invalid JSON message received"
  val notFound404 = "Resource not found"
  val serverError500 = "Server error"
  val sapError500 = "SAP_NUMBER missing or invalid"
  val noRegime500 = "REGIME missing or invalid"
  val notProcessed503 = "Request could not be processed on remote endpoint"
  val unavailble503 = "Service unavailable"
}
