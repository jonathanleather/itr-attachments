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

package flow

import akka.stream._
import akka.stream.stage._
import models.ValidationError
import util.RowValidator

/*
 * Custom Akka streaming flow graph stage that validates each row by calling RowValidator.validate on it.
 * The stage receives a stream of unvalidated rows.
 * For each input row it outputs a validation outcome, which is simply either just the incoming row
 * (if validation succeeded) or the validation errors (if validation failed)
 *
 * As a custom stage it can safely maintain mutable state, in this case it
 * keeps track of the row number for inclusion in validation error details
 *
 * @author David O'Riordan
 */
class ValidationStage extends GraphStage[FlowShape[Row, RowValidationOutcome]] {

  val in: Inlet[Row] = Inlet("unvalidated-row")
  val out: Outlet[RowValidationOutcome] = Outlet ("validated-row")

  override val shape = FlowShape.of(in,out)

  override def createLogic(inheritedAttributes: Attributes) : GraphStageLogic =
    new GraphStageLogic(shape) {
      private var rowNumber = 1

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val row = grab(in)
          val rowErrors: List[ValidationError] = RowValidator.validate(row, rowNumber)
          rowNumber += 1
          if (rowErrors.size > 0)
            push(out, Left(rowErrors)) // failed validation
          else
            push(out, Right(row)) // passed validation
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
        pull(in)
      }
    })
  }
}
