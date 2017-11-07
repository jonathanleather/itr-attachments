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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class InvestorHolding(
  dateIssued: String,
  description: String,
  amount: Double,
  number: Int)

object InvestorHolding {
  implicit val writes = Json.writes[InvestorHolding]
}

case class InvestorDetails(
  firstName: String,
  lastName: Option[String],
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  postCode: Option[String],
  country: String,
  nomineeFlag: Boolean,
  holdings: List[InvestorHolding])

object InvestorDetails {

  implicit val writes = Json.writes[InvestorDetails]

    implicit val mongoInnerWrites = OWrites[InvestorHolding] { investorHolding =>
      Json.obj(
        "dateIssued" -> investorHolding.dateIssued,
        "description" -> investorHolding.description,
        "amount" -> investorHolding.amount,
        "number" -> investorHolding.number
      )
    }

    implicit val mongoWrites = OWrites[InvestorDetails] { investorDetails =>
      Json.obj(
        "firstName" -> investorDetails.firstName,
        "lastName" -> investorDetails.lastName,
        "addressLine1" -> investorDetails.addressLine1,
        "addressLine2" -> investorDetails.addressLine2,
        "addressLine3" -> investorDetails.addressLine3,
        "postCode" -> investorDetails.postCode,
        "country" -> investorDetails.country,
        "nomineeFlag" -> investorDetails.nomineeFlag,
        "holdings" -> investorDetails.holdings
      )
    }

    implicit val mongoReads: Reads[InvestorDetails] = (
      (JsPath \ "firstName").read[String] and
        (JsPath \ "lastName").readNullable[String] and
        (JsPath \ "addressLine1").read[String] and
        (JsPath \ "addressLine2").read[String] and
        (JsPath \ "addressLine3").readNullable[String] and
        (JsPath \ "postCode").readNullable[String] and
        (JsPath \ "country").read[String] and
          (JsPath \ "nomineeFlag").read[Boolean] and
        __.lazyRead(Reads.list[InvestorHolding](mongoInnerReads))
      ) ((firstName, lastName, addressLine1, addressLine2, addressLine3, postCode, country, nomineeFlag, holdings) => InvestorDetails(firstName, lastName, addressLine1, addressLine2, addressLine3, postCode, country, nomineeFlag, holdings))

  implicit val mongoInnerReads: Reads[InvestorHolding] =(
    (JsPath \"dateIssued").read[String] and
      (JsPath \"description").read[String] and
      (JsPath \"amount").read[Double] and
      (JsPath \"number").read[Int]
    )(InvestorHolding.apply _)

    val mongoFormats = Format(mongoReads, mongoWrites)


}
