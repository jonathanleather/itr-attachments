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

import akka.stream.scaladsl._
import models.{InvestorDetails, InvestorHolding}
import util.Util.canonicalize

object TransformationStage {

  /*
   * Flow to transform an investor details row into an InvestorDetails object
   */
  val rowToInvestorDetails = Flow[Row].map[InvestorDetails] { row =>
    val numberOfHoldings = (row.size - 8) / 4
    val holdingsUnparsed = row.drop(8).grouped(4)
    val holdings = holdingsUnparsed.map(fromHoldingColumns)

    InvestorDetails(
      canonicalize(row.head),
      optional(row(1)).map(canonicalize),
      canonicalize(row(2)),
      canonicalize(row(3)),
      optional(row(4)).map(canonicalize),
      optional(row(5)).map(canonicalize),
      canonicalize(row(6)),
      isNominee(row(7)),
      holdings.toList)
    }

  val fromHoldingColumns: Seq[String] => InvestorHolding = { fields =>
    InvestorHolding(
      canonicalize(fields.head),
      canonicalize(fields(1)),
      fields(2).toDouble,
      fields(3).toInt
    )
  }

  private def optional(s: String): Option[String] = if (s.length > 0) Some(s) else None
  private def isNominee(flag: String) =  flag.toUpperCase == "YES"

}
