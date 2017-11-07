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

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import models.{InvestorDetails, ValidationError}
import util.Util

import scala.concurrent.Future

/*
 * @author David O'Riordan
 */
package object flow {
  type Row=Seq[String]
  type RowErrors=List[ValidationError]
  type RowValidationOutcome=Either[RowErrors,Row] // type of output of validation stage

  type OutputSink = Sink[InvestorDetails,Future[Done]]
  type ErrorSink  = Sink[List[ValidationError],Future[IOResult]]

  object CSV {
    type File = Source[ByteString, NotUsed]
    val Separator = ","
  }

  implicit val system = ActorSystem("InvestorDetailsFlow")
  implicit val materializer = ActorMaterializer()

  /*
   * Create an akka streaming graph that parses, validates and transforms an investor
   * details CSV file
   * It has a single inlet (for the input CSV file bytes) and two outlets (one for transformed, valid details
   * and one for any validation errors)
   */
  def investorDetailsCSVFlow(csv: CSV.File, outputSink: OutputSink, errorSink: ErrorSink) =
    GraphDSL.create(outputSink, errorSink)((_,_)) { implicit builder =>
      (outputSink, errorSink) =>
        import GraphDSL.Implicits._

        val parse = builder.add(parseFromCSV)

        val validate = builder.add(new ValidationStage)

        val postValidation = builder.add(Broadcast[RowValidationOutcome](2)) // replace with Partition?

        val collectValidInvestorDetails = builder.add(Flow[RowValidationOutcome].collect {
          case Right(details) => details
        })

        val transform = builder.add(TransformationStage.rowToInvestorDetails)

        val collectValidationErrors=builder.add(Flow[RowValidationOutcome].collect {
          case Left(err) => err
        })

        csv ~> parse ~> validate ~> postValidation ~> collectValidInvestorDetails  ~> transform ~> outputSink // happy flow
                                    postValidation ~> collectValidationErrors ~> errorSink // unhappy subflow

        ClosedShape
    }

  def parseFromCSV = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), 10000, false))
    .map(_.utf8String)
    .map(Util.splitRowIntoColumns(_).toSeq)
}
